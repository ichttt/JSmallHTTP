package de.nocoffeetech.smallhttp.internal.handler;

import org.openjdk.jmh.infra.Blackhole;

import java.io.OutputStream;

public class BlackholeOutputStream extends OutputStream {
    private final Blackhole blackhole;

    public BlackholeOutputStream(Blackhole blackhole) {
        this.blackhole = blackhole;
    }

    @Override
    public void write(int b) {
        this.blackhole.consume(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        this.blackhole.consume(b);
        this.blackhole.consume(off);
        this.blackhole.consume(len);
    }
}
