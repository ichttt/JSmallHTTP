package de.nocoffeetech.smallhttp.internal.handler;

import java.io.IOException;
import java.io.OutputStream;

public class NoCloseOutputStreamWrapper extends OutputStream {
    private final OutputStream toWrap;

    public NoCloseOutputStreamWrapper(OutputStream toWrap) {
        this.toWrap = toWrap;
    }

    @Override
    public void write(int b) throws IOException {
        toWrap.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        toWrap.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        toWrap.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        toWrap.flush();
    }

    @Override
    public void close() throws IOException {
        // Nothing!
    }
}
