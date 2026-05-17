use std::borrow::Cow;
use std::ffi::CStr;
use std::fs;
use std::os::raw::c_char;
use std::path::Path;

#[unsafe(no_mangle)]
pub extern "C" fn add_numbers(a: i32, b: i32) -> i32 {
    a + b
}

#[unsafe(no_mangle)]
pub extern "C" fn print_message(c_buf: *const c_char) {
    let c_str = unsafe { CStr::from_ptr(c_buf) };
    if let Ok(s) = c_str.to_str() {
        println!("Rust received: {}", s);
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn parse_csv(c_buf: *const c_char) {
    if c_buf.is_null() {
        eprintln!("Error: Received null pointer.");
        return;
    }

    let file_name_c = unsafe { CStr::from_ptr(c_buf) };

    if let Ok(file_name) = file_name_c.to_str() {
        match fs::read(Path::new(file_name)) {
            Ok(file_contents) => {
                let msg = String::from_utf8_lossy(&file_contents);
                let parsed = parse_chaotic_csv(&msg);

                for row in parsed {
                    println!("{:?}", row.get(2));
                }
            }
            Err(e) => eprintln!("Error when reading file {}: {}", file_name, e),
        }
    } else {
        eprintln!("Error: Invalid UTF-8 sequence in path.");
    }
}

fn parse_chaotic_csv(csv_data: &Cow<str>) -> Vec<Vec<String>> {
    let mut records = Vec::new();

    for line in csv_data.lines() {
        let line = line.trim();
        if line.is_empty() {
            continue;
        }

        // 1. Detect the delimiter for this specific line
        let delimiter = detect_delimiter(line);

        // 2. Clean or split the line based on the chaos level
        let fields = split_and_clean_line(line, delimiter);
        records.push(fields);
    }

    records
}

fn detect_delimiter(line: &str) -> char {
    // Count occurrences to guess the delimiter for this line
    let commas = line.chars().filter(|&c| c == ',').count();
    let semicolons = line.chars().filter(|&c| c == ';').count();

    if semicolons > commas { ';' } else { ',' }
}

fn split_and_clean_line(line: &str, delimiter: char) -> Vec<String> {
    // Split by the detected delimiter
    line.split(delimiter)
        .map(|field| {
            let mut cleaned = field.trim();

            // Strip leading/trailing single or double quotes safely
            if (cleaned.starts_with('"') && cleaned.ends_with('"')) ||
                (cleaned.starts_with('\'') && cleaned.ends_with('\'')) {
                if cleaned.len() >= 2 {
                    cleaned = &cleaned[1..cleaned.len() - 1];
                }
            }

            // Handle edge case: broken quotes (e.g., "value or value')
            cleaned = cleaned.trim_matches(|c| c == '"' || c == '\'');

            cleaned.to_string()
        })
        .collect()
}
