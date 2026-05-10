package io.csv;

import com.univocity.parsers.csv.*;

public class CsvLineParser {

    private final CsvParser parser;

    public CsvLineParser() {

        CsvParserSettings settings = new CsvParserSettings();

        settings.getFormat().setQuote('"');
        settings.getFormat().setQuoteEscape('"');
        settings.setMaxCharsPerColumn(10_000);
        settings.setLineSeparatorDetectionEnabled(true);
        settings.setSkipEmptyLines(true);

        this.parser = new CsvParser(settings);
    }

    public String[] parse(String line) {
        return parser.parseLine(line);
    }
}