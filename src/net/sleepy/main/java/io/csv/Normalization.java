package net.sleepy.main.java.io.csv;

import java.util.Map;

public class Normalization {

    public static final Map<String, String> aliases = Map.of(
            "населено място", "city",
            "търговски обект", "store",
            "наименование на продукта", "product_name",
            "код на продукта", "product_code",
            "категория", "category",
            "цена на дребно", "price_retail",
            "цена в промоция", "price_promo"
    );

    public static String norm (String input) {
        return input == null ? null :
                input.toLowerCase()
                .replace("\uFEFF", "")
                .replace("\"", "")
                .trim();
    }

    public static String normBussName (String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String[] parts = input.split("[(_]");

        return parts[0].trim()
                .toLowerCase()
                .replace(" ", "_");
    }

    public static char detectDelimiter (String line) {

        boolean inQuotes = false;

        int commas = 0;
        int semicolons = 0;

        for (char c : line.toCharArray()) {

            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }

            if (!inQuotes) {

                if (c == ',') {
                    commas++;
                }

                if (c == ';') {
                    semicolons++;
                }
            }
        }

        return semicolons > commas ? ';' : ',';
    }

    public static String sanitizeBrokenQuotes(String input) {

        StringBuilder fixed = new StringBuilder();
        int quoteCount = 0;

        for (char c : input.toCharArray()) {
            if (c == '"') quoteCount++;
            fixed.append(c);
        }

        // If odd number of quotes → likely broken row
        if (quoteCount % 2 != 0) {
            fixed.append('"'); // close dangling quote
        }

        return fixed.toString();
    }

    public static String repairLine(String line) {
        int quotes = 0;

        for (char c : line.toCharArray()) {
            if (c == '"') quotes++;
        }

        // If quotes are unbalanced, try to neutralize obvious corruption
        if (quotes % 2 != 0) {
            line = line + "\"";
        }

        return line;
    }
}
