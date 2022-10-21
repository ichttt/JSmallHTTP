package de.umweltcampus.smallhttp.internal.handler;

import de.umweltcampus.smallhttp.data.HTTPVersion;

import java.io.OutputStream;

public class TestResponseWriter extends ResponseWriter {
    public TestResponseWriter(OutputStream stream, ReusableClientContext context, HTTPVersion requestVersion) {
        super(stream, context, requestVersion);
    }

    @Override
    protected String getServerDate() {
        return "DUMMYDATE";
    }
}
