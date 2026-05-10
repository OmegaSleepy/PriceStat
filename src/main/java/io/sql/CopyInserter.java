package io.sql;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import java.io.StringReader;
import java.sql.Connection;

public class CopyInserter {

    private final StringBuilder buffer = new StringBuilder();
    private final CopyManager copyManager;

    private static final int FLUSH_SIZE = 50_000;

    public CopyInserter(Connection connection) throws Exception {
        this.copyManager = new CopyManager(connection.unwrap(BaseConnection.class));
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

        if (buffer.length() > FLUSH_SIZE) {
            flush();
        }
    }

    public void flush() {
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
                    firm
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