package org.iz.cs.chunker;

import java.io.PrintStream;

public class ConsolePrinter {

    private static PrintStream out;

    private ConsolePrinter() {
        // No instances
    }

    public static void println(String x) {
        out.println(x);
    }

    public static void setOut(PrintStream out) {
        ConsolePrinter.out = out;
    }

}
