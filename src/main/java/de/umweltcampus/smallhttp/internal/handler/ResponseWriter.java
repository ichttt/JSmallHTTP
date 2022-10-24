package de.umweltcampus.smallhttp.internal.handler;

import de.umweltcampus.smallhttp.data.HTTPVersion;
import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.BuiltinHeaders;
import de.umweltcampus.smallhttp.header.CommonContentTypes;
import de.umweltcampus.smallhttp.header.PrecomputedHeader;
import de.umweltcampus.smallhttp.header.PrecomputedHeaderKey;
import de.umweltcampus.smallhttp.response.ResponseBodyWriter;
import de.umweltcampus.smallhttp.response.ResponseHeaderWriter;
import de.umweltcampus.smallhttp.response.ResponseStartWriter;
import de.umweltcampus.smallhttp.response.ResponseToken;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class ResponseWriter implements ResponseStartWriter, ResponseHeaderWriter, ResponseBodyWriter {
    private static final Calendar CALENDAR = Calendar.getInstance();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ROOT);
    private static final PrecomputedHeader SERVER_HEADER = new PrecomputedHeader(BuiltinHeaders.SERVER.headerKey, "JSmallHTTP");
    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

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
        this.addHeader(BuiltinHeaders.CONTENT_LENGTH.headerKey, size + "");

        sendHeader();
        return this;
    }

    @Override
    public ResponseBodyWriter beginBodyWithUnknownSize() throws IOException {
        if (this.startedSendingData || this.status == null) throw new IllegalStateException();
        if (true) throw new RuntimeException("Chunked transfer is not implemented (yet)!");

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
    public ResponseToken finalizeResponse() throws IOException {
        if (completed) throw new IllegalStateException();

        if (!startedSendingData) sendHeader();
        this.stream.flush();
        this.completed = true;
        return ResponseTokenImpl.get();
    }

    /**
     * Gets the server date in the HTTP format. Protected so it can be changed for reproducible tests
     * @return
     */
    protected String getServerDate() {
        // Format according to https://www.rfc-editor.org/rfc/rfc9110#section-5.6.7
        return DATE_FORMAT.format(CALENDAR.getTime());
    }


    private void sendHeader() throws IOException {
        // Prepare remaining builtin headers
        byte[] dateKey = BuiltinHeaders.DATE.headerKey.asciiBytes;
        byte[] dateValue = getServerDate().getBytes(StandardCharsets.US_ASCII);
        byte[] serverHeader = SERVER_HEADER.asciiBytes;

        // Point of no return
        this.startedSendingData = true;
        // Start with status line (https://www.rfc-editor.org/rfc/rfc9112#name-status-line)
        OutputStream stream = this.stream;
        this.requestVersion.writeToHeader(stream);
        this.status.writeToHeader(stream);
        // Continue with builtin headers and write them directly

        // Date header
        stream.write(dateKey);
        stream.write(':');
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
    }
}
