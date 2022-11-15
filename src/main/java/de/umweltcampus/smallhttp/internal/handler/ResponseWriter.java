package de.umweltcampus.smallhttp.internal.handler;

import de.umweltcampus.smallhttp.data.HTTPVersion;
import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.BuiltinHeaders;
import de.umweltcampus.smallhttp.header.CommonContentTypes;
import de.umweltcampus.smallhttp.header.PrecomputedHeader;
import de.umweltcampus.smallhttp.header.PrecomputedHeaderKey;
import de.umweltcampus.smallhttp.internal.util.ResponseDateFormatter;
import de.umweltcampus.smallhttp.response.ChunkedResponseWriter;
import de.umweltcampus.smallhttp.response.FixedResponseBodyWriter;
import de.umweltcampus.smallhttp.response.HTTPWriteException;
import de.umweltcampus.smallhttp.response.ResponseHeaderWriter;
import de.umweltcampus.smallhttp.response.ResponseStartWriter;
import de.umweltcampus.smallhttp.response.ResponseToken;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;

public class ResponseWriter implements ResponseStartWriter, ResponseHeaderWriter, FixedResponseBodyWriter, ChunkedResponseWriter {
    private static final PrecomputedHeader SERVER_HEADER = new PrecomputedHeader(BuiltinHeaders.SERVER.headerKey, "JSmallHTTP");
    private static final PrecomputedHeader CHUNKED_ENCODING = new PrecomputedHeader(BuiltinHeaders.TRANSFER_ENCODING.headerKey, "chunked");
    private static final byte[] CRLF_BYTES = "\r\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] TRANSFER_ENCODING_END = "0\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

    private final OutputStream stream;
    private final SocketChannel channel;
    private final byte[] responseBuffer;
    private final Clock clock;
    private final HTTPVersion requestVersion;
    private int responseBufferNextIndex = 0;
    private Status status = null;
    private boolean startedSendingData = false;
    private boolean completed = false;
    private boolean chunked = false;

    public ResponseWriter(OutputStream stream, SocketChannel channel, ReusableClientContext context, HTTPVersion requestVersion) {
        this.stream = stream;
        this.channel = channel;
        this.responseBuffer = context.responseBuffer;
        this.clock = context.clock;
        this.requestVersion = requestVersion;
    }

    @Override
    public ResponseHeaderWriter respond(Status status, String contentType) {
        if (status == null || contentType == null) throw new IllegalArgumentException();
        if (this.status != null) throw new IllegalStateException();

        this.status = status;
        this.addHeader(BuiltinHeaders.CONTENT_TYPE.headerKey, contentType);
        return this;
    }

    @Override
    public ResponseHeaderWriter respond(Status status, CommonContentTypes contentType) {
        if (status == null) throw new IllegalArgumentException();
        if (this.status != null) throw new IllegalStateException();

        this.status = status;
        this.addHeader(contentType.header);
        return this;
    }

    @Override
    public ResponseHeaderWriter respondWithoutContentType(Status status) {
        if (status == null) throw new IllegalArgumentException();
        if (this.status != null) throw new IllegalStateException();

        this.status = status;
        return this;
    }

    @Override
    public ResponseHeaderWriter addHeader(PrecomputedHeader header) {
        byte[] completeAsciiBytes = header.asciiBytes;
        int requiredLength = responseBufferNextIndex + completeAsciiBytes.length;
        if (requiredLength > responseBuffer.length) {
            throw new RuntimeException("Headers get too large, requested size is " + requiredLength + ", but buffer can only fit " + responseBuffer.length + " !");
        }
        System.arraycopy(completeAsciiBytes, 0, responseBuffer, responseBufferNextIndex, completeAsciiBytes.length);
        responseBufferNextIndex += completeAsciiBytes.length;

        return this;
    }

    @Override
    public ResponseHeaderWriter addHeader(PrecomputedHeaderKey headerKey, String value) {
        if (headerKey == null || value == null) throw new IllegalArgumentException();
        if (this.startedSendingData || this.status == null) throw new IllegalStateException();

        byte[] responseBuffer = this.responseBuffer;
        byte[] nameBytes = headerKey.asciiBytes;
        byte[] valueBytes = value.getBytes(StandardCharsets.US_ASCII);
        int requiredLength = responseBufferNextIndex + valueBytes.length + nameBytes.length + 3;
        if (requiredLength > responseBuffer.length) {
            throw new RuntimeException("Headers get too large, requested size is " + requiredLength + ", but buffer can only fit " + responseBuffer.length + " !");
        }

        System.arraycopy(nameBytes, 0, responseBuffer, responseBufferNextIndex, nameBytes.length);
        responseBufferNextIndex += nameBytes.length;

        System.arraycopy(valueBytes, 0, responseBuffer, responseBufferNextIndex, valueBytes.length);
        responseBufferNextIndex += valueBytes.length;

        responseBuffer[responseBufferNextIndex] = '\r';
        responseBuffer[responseBufferNextIndex + 1] = '\n';
        responseBufferNextIndex += 2;

        return this;
    }

    @Override
    public FixedResponseBodyWriter beginBodyWithKnownSize(long size) throws HTTPWriteException {
        if (size < 0) throw new IllegalArgumentException();
        if (this.startedSendingData || this.status == null) throw new IllegalStateException();
        this.addHeader(BuiltinHeaders.CONTENT_LENGTH.headerKey, size + "");

        sendHeader();
        return this;
    }

    @Override
    public ChunkedResponseWriter beginBodyWithUnknownSize() throws HTTPWriteException {
        if (this.startedSendingData || this.status == null) throw new IllegalStateException();
        if (this.requestVersion == HTTPVersion.HTTP_1_0) throw new IllegalStateException("Chunked transfer is forbidden for HTTP/1.0!");
        this.chunked = true;
        this.addHeader(CHUNKED_ENCODING);

        sendHeader();
        return this;
    }

    @Override
    public ResponseToken sendWithoutBody() throws HTTPWriteException {
        if (this.startedSendingData || this.status == null) throw new IllegalStateException();

        sendHeader();
        return finalizeResponse();
    }

    public ResponseStartWriter resetResponseBuilder() {
        if (this.startedSendingData) throw new IllegalStateException();

        this.responseBufferNextIndex = 0;
        this.status = null;
        return this;
    }



    @Override
    public OutputStream getRawOutputStream() {
        if (!this.startedSendingData || completed || chunked) throw new IllegalStateException();

        return new NoCloseOutputStreamWrapper(this.stream);
    }

    @Override
    public SocketChannel getRawSocketChannel() {
        if (!this.startedSendingData || completed || chunked) throw new IllegalStateException();

        return this.channel;
    }

    @Override
    public void writeChunk(byte[] data) throws HTTPWriteException {
        this.writeChunk(data, 0, data.length);
    }

    @Override
    public void writeChunk(byte[] data, int offset, int length) throws HTTPWriteException {
        if (!this.startedSendingData || completed || !chunked) throw new IllegalStateException();
        assert data.length - offset >= length;

        // See https://www.rfc-editor.org/rfc/rfc9112#name-chunked-transfer-coding
        byte[] lengthIndicator = Integer.toHexString(length).getBytes(StandardCharsets.US_ASCII);
        try {
            this.stream.write(lengthIndicator);
            this.stream.write(CRLF_BYTES);
            this.stream.write(data, offset, length);
            this.stream.write(CRLF_BYTES);
        } catch (IOException e) {
            throw new HTTPWriteException(e);
        }
    }

    @Override
    public ResponseToken finalizeResponse() throws HTTPWriteException {
        if (completed || !startedSendingData) throw new IllegalStateException();

        try {
            if (chunked) {
                this.stream.write(TRANSFER_ENCODING_END); //write the last chunk, see https://www.rfc-editor.org/rfc/rfc9112#name-chunked-transfer-coding
            }
            this.stream.flush();
        } catch (IOException e) {
            throw new HTTPWriteException(e);
        }
        this.completed = true;
        return ResponseTokenImpl.get();
    }


    private void sendHeader() throws HTTPWriteException {
        // Prepare remaining builtin headers
        byte[] dateKey = BuiltinHeaders.DATE.headerKey.asciiBytes;
        byte[] dateValue = ResponseDateFormatter.format(LocalDateTime.now(clock)).getBytes(StandardCharsets.US_ASCII);
        byte[] serverHeader = SERVER_HEADER.asciiBytes;

        // Point of no return
        this.startedSendingData = true;
        // Start with status line (https://www.rfc-editor.org/rfc/rfc9112#name-status-line)
        OutputStream stream = this.stream;

        try {
            this.requestVersion.writeToHeader(stream);
            this.status.writeToHeader(stream);
            // Continue with builtin headers and write them directly

            // Date header
            stream.write(dateKey);
            stream.write(dateValue);
            stream.write('\r');
            stream.write('\n');

            // Server header
            stream.write(serverHeader);

            // Write the other header bytes
            stream.write(this.responseBuffer, 0, this.responseBufferNextIndex);
            // End with a last CR LF to signal start of body
            stream.write('\r');
            stream.write('\n');
        } catch (IOException e) {
            throw new HTTPWriteException(e);
        }
    }
}
