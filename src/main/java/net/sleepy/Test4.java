package net.sleepy;

import net.sleepy.nativebind.bindings_h;
import java.nio.file.Paths;

public class Test4 {
    static {
        // No path or extension needed; Java looks for 'lib' + 'price_stat' + '.so'
        System.loadLibrary("price_stat");
    }

    public static void main(String[] args) {
        System.out.println("Result: " + bindings_h.add_numbers(5, 10));

        try (var arena = java.lang.foreign.Arena.ofConfined()) {
            var cString = arena.allocateFrom("Hello from the JVM!");
            bindings_h.print_message(cString);
        }
    }
}