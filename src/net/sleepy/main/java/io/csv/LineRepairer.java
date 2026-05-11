package net.sleepy.main.java.io.csv;

public class LineRepairer {

    public static String repair(String line) {

        if (line == null || line.isBlank()) return line;

        // Fix broken quote balancing
        long quotes = line.chars().filter(c -> c == '"').count();

        if (quotes % 2 != 0) {
            line = line + "\"";
        }

        // Remove known corruption pattern from your dataset
        line = line.replace(";\";\"", ";\"");

        return line;
    }
}