package de.umweltcampus.smallhttp.handler;

import de.umweltcampus.smallhttp.data.HTTPVersion;
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
    private final HTTPVersion requestVersion;
    private int responseBufferNextIndex = 0;
    private Status status = null;
    private boolean startedSendingData = false;
    private boolean completed = false;

    public ResponseWriter(OutputStream stream, ReusableClientContext context, HTTPVersion requestVersion) {
        this.stream = stream;
        this.responseBuffer = context.responseBuffer;
        this.requestVersion = requestVersion;
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

        byte[] responseBuffer = this.responseBuffer;
        byte[] nameBytes = name.getBytes();
        byte[] valueBytes = value.getBytes(StandardCharsets.US_ASCII);
        int requiredLength = responseBufferNextIndex + valueBytes.length + nameBytes.length + 3;
        if (requiredLength > responseBuffer.length) {
            throw new RuntimeException("Headers get too large, requested size is " + requiredLength + ", but buffer can only fit " + responseBuffer.length + " !");
        }

        System.arraycopy(nameBytes, 0, responseBuffer, responseBufferNextIndex, nameBytes.length);
        responseBufferNextIndex += nameBytes.length;

        responseBuffer[responseBufferNextIndex] = ':';
        responseBufferNextIndex++;

        System.arraycopy(valueBytes, 0, responseBuffer, responseBufferNextIndex, valueBytes.length);
        responseBufferNextIndex += valueBytes.length;

        responseBuffer[responseBufferNextIndex] = '\r';
        responseBuffer[responseBufferNextIndex + 1] = '\n';
        responseBufferNextIndex += 2;

        return this;
    }

    @Override
    public ResponseBodyWriter beginBodyWithKnownSize(int size) throws IOException {
        if (size < 0) throw new IllegalArgumentException();
        if (this.startedSendingData || this.status == null) throw new IllegalStateException();

        sendHeader();
        return this;
    }

    @Override
    public ResponseBodyWriter beginBodyWithUnknownSize() throws IOException {
        if (this.startedSendingData || this.status == null) throw new IllegalStateException();

        sendHeader();
        return this;
    }

    public ResponseStartWriter resetResponseBuilder() {
        if (this.startedSendingData) throw new IllegalStateException();

        this.responseBufferNextIndex = 0;
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


    private void sendHeader() throws IOException {
        this.startedSendingData = true;
        // Start with status line (https://www.rfc-editor.org/rfc/rfc9112#name-status-line)
        OutputStream stream = this.stream;
        this.requestVersion.writeToHeader(stream);
        this.status.writeToHeader(stream);
        // Write the header bytes
        this.stream.write(this.responseBuffer, 0, responseBufferNextIndex);
        // End with a last CR LF to signal start of body
        this.stream.write('\r');
        this.stream.write('\n');
    }
}
