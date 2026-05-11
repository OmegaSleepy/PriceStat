package net.sleepy.io.csv;

import java.nio.file.*;

@Deprecated
public class DeadLetterStore {

    private static final Path file = Path.of("bad_rows.log");

    public static synchronized void record(String line, Exception e) {
        try {
            Files.writeString(
                    file,
                    line + " | ERROR: " + e.getMessage() + "\n",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (Exception ignored) {}
    }
}