package de.umweltcampus.smallhttp.internal.handler;

import de.umweltcampus.smallhttp.base.ErrorHandler;
import de.umweltcampus.smallhttp.base.RequestHandler;
import de.umweltcampus.smallhttp.internal.watchdog.ClientHandlerTracker;
import org.openjdk.jmh.infra.Blackhole;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BenchmarkClientHandler extends HTTPClientHandler {
    private final ByteArrayInputStream inputStream;
    private final BlackholeOutputStream outputStream;

    public BenchmarkClientHandler(ErrorHandler errorHandler, RequestHandler handler, ClientHandlerTracker tracker, byte[] toRead, Blackhole blackhole) {
        super(null, errorHandler, handler, null, tracker, false, true, Short.MAX_VALUE);
        this.inputStream = new ByteArrayInputStream(toRead);
        this.outputStream = new BlackholeOutputStream(blackhole);
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        return outputStream;
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        return inputStream;
    }

    @Override
    protected void close() throws IOException {
        // nothing
    }
}
