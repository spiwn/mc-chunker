package org.iz.cs.chunker.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class OutputHandler extends PrintStream {

    public OutputHandler() {
        super(new NoOpOutputStream());
    }

    private static class NoOpOutputStream extends OutputStream {

        @Override
        public void write(byte[] b) throws IOException {
            // Do nothing
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            // Do nothing
        }

        @Override
        public void write(int b) throws IOException {
            // Do nothing
        }

    }

}
