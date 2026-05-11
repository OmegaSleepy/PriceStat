package net.sleepy.io.sql;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.sql.Connection;

@Deprecated
public class CopyInserter {

    private final StringBuilder buffer = new StringBuilder();
    private final CopyManager copyManager;
    private final String date;

    private final Logger logger = LoggerFactory.getLogger(CopyInserter.class);

    private static final int FLUSH_SIZE = 50_000;

    public CopyInserter(Connection connection, String date) throws Exception {
        this.copyManager = new CopyManager(connection.unwrap(BaseConnection.class));
        this.date = date;
    }

    public void add(String[] row, String firm) {

        if (row == null || row.length < 7) return;

        buffer.append(escape(row[0])).append('\t');
        buffer.append(escape(row[1])).append('\t');
        buffer.append(escape(row[2])).append('\t');
        buffer.append(escape(row[3])).append('\t');
        buffer.append(escape(row[4])).append('\t');
        buffer.append(parseNumber(row[5])).append('\t');
        buffer.append(parseNumber(row[6])).append('\t');
        buffer.append(escape(firm)).append('\n');
        buffer.append(escape(date)).append('\n');

        if (buffer.length() > FLUSH_SIZE) {
            flush();
        }
    }

    public void flush() {

        logger.debug("Flushing {} rows", buffer.length());
        try {
            if (buffer.isEmpty()) return;

            copyManager.copyIn("""
                COPY product (
                    city,
                    shop_address,
                    product_name,
                    product_id,
                    product_category,
                    product_price,
                    promo_price,
                    firm,
                    snapshot_date
                )
                FROM STDIN WITH (FORMAT text)
                """,
                new StringReader(buffer.toString())
            );

            buffer.setLength(0);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String escape(String value) {
        if (value == null || value.isBlank()) return "\\N";

        return value
                .replace("\\", "\\\\")
                .replace("\t", " ")
                .replace("\n", " ");
    }

    private String parseNumber(String value) {
        if (value == null) return "\\N";

        value = value.trim().replace(",", ".");

        try {
            return Double.toString(Double.parseDouble(value));
        } catch (Exception e) {
            return "\\N";
        }
    }
}