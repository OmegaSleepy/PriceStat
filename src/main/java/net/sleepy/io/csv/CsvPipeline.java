package net.sleepy.io.csv;

import net.sleepy.io.sql.CopyInserter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class CsvPipeline {

    private static final CsvLineParser parser = new CsvLineParser();
    private static final Logger logger = LoggerFactory.getLogger(CsvPipeline.class);

    public static void process (String rawContent, String firm, CopyInserter inserter) {

        String[] lines = rawContent.split("\n");
        logger.info("Firm {} has lines {}", firm, lines.length);
        long start = System.currentTimeMillis();

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

        logger.info("Finished processing {}.", firm);
        long end = System.currentTimeMillis();
        logger.info("Time taken: {} ms.", end - start);
    }
}