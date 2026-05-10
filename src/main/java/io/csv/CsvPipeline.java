package io.csv;

import io.sql.CopyInserter;

public class CsvPipeline {

    private static final CsvLineParser parser = new CsvLineParser();

    public static void process(String rawContent, String firm, CopyInserter inserter) {

        String[] lines = rawContent.split("\n");

        for (String line : lines) {

            line = LineRepairer.repair(line);

            try {
                String[] row = parser.parse(line);

                if (row == null || row.length < 7) continue;

                inserter.add(row, firm);

            } catch (Exception e) {
                DeadLetterStore.record(line, e);
            }
        }
    }
}