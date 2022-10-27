package de.umweltcampus.smallhttp.response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ChunkedResponseWriter {

    void writeChunk(byte[] data) throws IOException;

    void writeChunk(byte[] data, int offset, int length) throws IOException;

    default void writeFromInputStream(InputStream stream) throws IOException {
        byte[] buffer = new byte[32768]; // fairly large buffer to limit chunk amounts to avoid waste
        int read;
        while ((read = stream.read(buffer)) >= 0) {
            writeChunk(buffer, 0, read);
        }
    }

    /**
     * Finalizes the response and flushes it to the client.
     * This must be called when the response is completed.
     * @return A response token that must be returned, marking the response as complete
     */
    ResponseToken finalizeResponse() throws IOException;
}
