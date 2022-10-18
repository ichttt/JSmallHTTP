package de.umweltcampus.smallhttp.handler;

import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.response.ResponseBodyWriter;
import de.umweltcampus.smallhttp.response.ResponseHeaderWriter;
import de.umweltcampus.smallhttp.response.ResponseStartWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ResponseWriter implements ResponseStartWriter, ResponseHeaderWriter, ResponseBodyWriter {
    private final OutputStream stream;
    private final byte[] responseBuffer;
    private int responseBufferIndex = 0;
    private Status status = null;
    private boolean startedSendingData = false;
    private boolean completed = false;

    public ResponseWriter(OutputStream stream, ReusableClientContext context) {
        this.stream = stream;
        this.responseBuffer = context.responseBuffer;
    }

    @Override
    public ResponseHeaderWriter respond(Status status) {
        if (status == null) throw new IllegalArgumentException();
        if (this.status != null) throw new IllegalStateException();

        this.status = status;
        return this;
    }



    @Override
    public ResponseHeaderWriter addHeader(String name, String value) {
        if (name == null || value == null) throw new IllegalArgumentException();
        if (this.startedSendingData || this.status == null) throw new IllegalStateException();

        // TODO
        return this;
    }

    @Override
    public ResponseBodyWriter beginBodyWithKnownSize(int size) {
        if (size < 0) throw new IllegalArgumentException();
        if (this.startedSendingData || this.status == null) throw new IllegalStateException();

        sendHeader();
        return this;
    }

    @Override
    public ResponseBodyWriter beginBodyWithUnknownSize() {
        if (this.startedSendingData || this.status == null) throw new IllegalStateException();

        sendHeader();
        return this;
    }

    public ResponseStartWriter resetResponseBuilder() {
        if (this.startedSendingData) throw new IllegalStateException();

        this.responseBufferIndex = 0;
        this.status = null;
        return this;
    }



    @Override
    public OutputStream getRawOutputStream() {
        if (!this.startedSendingData || completed) throw new IllegalStateException();

        return this.stream;
    }

    @Override
    public void writeString(String msg) throws IOException {
        if (!this.startedSendingData || completed) throw new IllegalStateException();

        this.stream.write(msg.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void finalizeResponse() throws IOException {
        if (completed) return;

        if (!startedSendingData) sendHeader();
        this.stream.flush();
        this.completed = true;
    }


    private void sendHeader() {
        this.startedSendingData = true;
        // TODO
    }
}
