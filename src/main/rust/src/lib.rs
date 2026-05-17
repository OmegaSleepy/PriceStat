use std::borrow::Cow;
use std::ffi::CStr;
use std::fs::File;
use std::io::Read;
use std::os::raw::c_char;
use std::path::Path;

use deadpool_postgres::{Manager, Pool, Runtime as DeadpoolRuntime};
use once_cell::sync::Lazy;
use tokio::runtime::Runtime;

use chrono::{NaiveDate, NaiveDateTime, Utc};
use futures_util::pin_mut;
use tokio::task;
use tokio_postgres::binary_copy::BinaryCopyInWriter;
use tokio_postgres::types::ToSql;

// Structure to carry file content metadata
struct CsvFile {
    name: String,
    content: Cow<'static, str>,
}

#[unsafe(no_mangle)]
pub extern "C" fn parse_zip_csv(c_buf: *const c_char) {
    if c_buf.is_null() {
        eprintln!("Null pointer received");
        return;
    }

    let file_name_c = unsafe { CStr::from_ptr(c_buf) };

    let zip_path = match file_name_c.to_str() {
        Ok(v) => v.to_string(),
        Err(_) => {
            eprintln!("Invalid UTF-8 path");
            return;
        }
    };

    TOKIO_RUNTIME.block_on(async {
        process_zip(zip_path).await;
    });
}

fn parse_snapshot_date(zip_path: &str) -> (String, NaiveDateTime) {
    let path = Path::new(zip_path);
    let stem = path
        .file_stem()
        .and_then(|s| s.to_str())
        .unwrap_or("unknown_snapshot");

    // 1. Try parsing full timestamp first: "2026-05-16_14-30-00"
    let snapshot_date = if let Ok(dt) = NaiveDateTime::parse_from_str(stem, "%Y-%m-%d_%H-%M-%S") {
        dt
    }
    // 2. Fall back to just the date: "2026-05-16" and attach midnight time
    else if let Ok(date) = NaiveDate::parse_from_str(stem, "%Y-%m-%d") {
        date.and_hms_opt(0, 0, 0)
            .unwrap_or_else(|| Utc::now().naive_utc())
    }
    // 3. Ultimate fallback if the filename format is missing entirely
    else {
        Utc::now().naive_utc()
    };

    (stem.to_string(), snapshot_date)
}

fn create_db_pool() -> Pool {
    let mut pg_cfg = tokio_postgres::Config::new();

    pg_cfg.user("appuser");
    pg_cfg.password("apppassword");
    pg_cfg.dbname("appdb");
    pg_cfg.host("localhost");

    let mgr = Manager::new(pg_cfg, deadpool_postgres::tokio_postgres::NoTls);

    Pool::builder(mgr)
        .max_size(16)
        .runtime(DeadpoolRuntime::Tokio1)
        .build()
        .unwrap()
}

static DB_POOL: Lazy<Pool> = Lazy::new(create_db_pool);

static TOKIO_RUNTIME: Lazy<Runtime> =
    Lazy::new(|| Runtime::new().expect("Failed to create runtime"));

async fn process_zip(zip_path: String) {
    let (snapshot_name, snapshot_date) = parse_snapshot_date(&zip_path);

    // Open ZIP and extract CSV names and content in a blocking thread
    let csv_contents = match task::spawn_blocking(move || extract_csvs(zip_path)).await {
        Ok(Ok(files)) => files,
        Ok(Err(e)) => {
            eprintln!("ZIP error: {}", e);
            return;
        }
        Err(e) => {
            eprintln!("Task failed: {}", e);
            return;
        }
    };

    let pool = &*DB_POOL;
    let mut handles = Vec::new();

    for file in csv_contents {
        let pool = DB_POOL.clone();
        let snapshot_date = snapshot_date.clone();

        let handle = tokio::spawn(async move {
            let parsed_rows = parse_chaotic_csv(&file.content);
            let total_rows = parsed_rows.len();

            if total_rows <= 1 {
                return 0;
            }

            // Get a connection from the pool
            let client = match pool.get().await {
                Ok(c) => c,
                Err(e) => {
                    eprintln!("DB Pool error: {}", e);
                    return 0;
                }
            };

            // Using Postgres Binary COPY for raw injection speed
            let sink = match client
                .copy_in("COPY product (city, shop_address, product_name, product_id, product_category, product_price, promo_price, firm, snapshot_date) FROM STDIN BINARY")
                .await
            {
                Ok(s) => s,
                Err(e) => {
                    eprintln!("Failed to initialize COPY: {}", e);
                    return 0;
                }
            };

            let writer = BinaryCopyInWriter::new(
                sink,
                &[
                    tokio_postgres::types::Type::TEXT,
                    tokio_postgres::types::Type::TEXT,
                    tokio_postgres::types::Type::TEXT,
                    tokio_postgres::types::Type::TEXT,
                    tokio_postgres::types::Type::TEXT,
                    tokio_postgres::types::Type::FLOAT8,
                    tokio_postgres::types::Type::FLOAT8,
                    tokio_postgres::types::Type::TEXT,
                    tokio_postgres::types::Type::TIMESTAMP,
                ],
            );

            pin_mut!(writer);

            let firm = Some(&file.name);
            let s_date = Some(&snapshot_date);

            for row in parsed_rows.iter().skip(1) {
                // Zero-allocation extraction: extract everything as Option<&str>
                let city = row.get(0).map(|s| s.as_str());
                let shop_address = row.get(1).map(|s| s.as_str());
                let product_name = row.get(2).map(|s| s.as_str());
                let product_id = row.get(3).map(|s| s.as_str());
                let product_category = row.get(4).map(|s| s.as_str());

                // Parse numeric values directly from the references safely into Option<f64>
                let product_price: Option<f64> = row.get(5).and_then(|s| {
                    let sanitized = s.replace(',', ".");
                    sanitized.parse().ok()
                });

                let promo_price: Option<f64> = row.get(6).and_then(|s| {
                    let sanitized = s.replace(',', ".");
                    sanitized.parse().ok()
                });

                let values: &[&(dyn ToSql + Sync)] = &[
                    &city,
                    &shop_address,
                    &product_name,
                    &product_id,
                    &product_category,
                    &product_price,
                    &promo_price,
                    &firm,
                    &s_date,
                ];

                if let Err(e) = writer.as_mut().write(values).await {
                    eprintln!("Error writing row to binary stream: {}", e);
                    return 0;
                }
            }

            match writer.finish().await {
                Ok(_) => total_rows - 1,
                Err(e) => {
                    eprintln!("Error completing database COPY: {}", e);
                    0
                }
            }
        });

        handles.push(handle);
    }

    let mut total_rows = 0;
    for handle in handles {
        match handle.await {
            Ok(rows_inserted) => {
                total_rows += rows_inserted;
            }
            Err(e) => {
                eprintln!("Task panic: {}", e);
            }
        }
    }

    println!(
        "Finished processing snapshot '{}'. Total rows written to DB: {}",
        snapshot_name, total_rows
    );
}

fn extract_csvs(
    zip_path: String,
) -> Result<Vec<CsvFile>, Box<dyn std::error::Error + Send + Sync>> {
    let file = File::open(zip_path)?;
    let mut archive = zip::ZipArchive::new(file)?;

    let mut csv_files = Vec::new();

    for i in 0..archive.len() {
        let mut file = archive.by_index(i)?;

        if !file.name().ends_with(".csv") {
            continue;
        }

        // Keep just the file name (firm name) instead of full path inside zip if necessary
        let firm_name = Path::new(file.name())
            .file_name()
            .and_then(|s| s.to_str())
            .unwrap_or(file.name())
            .to_string();

        let mut buffer = Vec::new();
        file.read_to_end(&mut buffer)?;

        let text = String::from_utf8_lossy(&buffer).into_owned();

        csv_files.push(CsvFile {
            name: firm_name,
            content: Cow::Owned(text),
        });
    }

    Ok(csv_files)
}

fn parse_chaotic_csv(csv_data: &str) -> Vec<Vec<String>> {
    let mut records = Vec::new();

    // Peek at the first non-empty line to dynamically detect the delimiter
    let first_line = csv_data
        .lines()
        .find(|l| !l.trim().is_empty())
        .unwrap_or("");
    let delimiter = detect_delimiter(first_line);

    // Initialize the CSV reader configured with the correct delimiter
    let mut reader = csv::ReaderBuilder::new()
        .has_headers(false) // Assuming data has no headers based on your code
        .flexible(true) // Handles "chaotic" rows with different column counts
        .delimiter(delimiter as u8)
        .from_reader(csv_data.as_bytes());

    for result in reader.records() {
        match result {
            Ok(record) => {
                // Map fields to String. The csv crate automatically strips surrounding quotes
                let fields: Vec<String> = record.iter().map(|s| s.to_string()).collect();
                records.push(fields);
            }
            Err(e) => {
                eprintln!("Warning: Failed to parse CSV row: {}", e);
            }
        }
    }
    records
}

fn detect_delimiter(line: &str) -> char {
    let mut commas = 0;
    let mut semicolons = 0;
    let mut in_quotes = false;

    // Smart delimiter detection that ignores punctuation inside quotes
    for c in line.chars() {
        match c {
            '"' => in_quotes = !in_quotes,
            ',' if !in_quotes => commas += 1,
            ';' if !in_quotes => semicolons += 1,
            _ => {}
        }
    }
    if semicolons > commas { ';' } else { ',' }
}
