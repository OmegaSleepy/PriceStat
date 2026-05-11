package net.sleepy;

import net.sleepy.nativebind.bindings_h;


//TODO make an actual Rust interloop class
public final class PriceStat {

    static {
        String libPath = System.getProperty("user.dir")
                + "/src/main/rust/target/release/libprice_stat.so";

        System.load(libPath);
    }

    public static int add(int a, int b) {
        return bindings_h.add_numbers(a, b);
    }
}