package net.sleepy;

import net.sleepy.nativebind.bindings_h;

import java.lang.foreign.Arena;
import java.nio.file.Path;

public class RustFFM {

    public static void parseZip(Path file) {
        try (var arena = Arena.ofConfined()) {
            var cString = arena.allocateFrom(file.toAbsolutePath().toString());
            bindings_h.parse_zip_csv(cString);
        }
    }
}
