package de.umweltcampus.smallhttp.response;

import java.io.IOException;
import java.io.InputStream;

public interface ChunkedResponseWriter {

    /**
     * Writes a chunk of date to the client, automatically encoding with the correct transfer encoding
     * @param data The chunk to write
     * @throws HTTPWriteException If the write operation fails
     */
    void writeChunk(byte[] data) throws HTTPWriteException;

    /**
     * Writes a chunk of date to the client, automatically encoding with the correct transfer encoding
     * @param data The chunk to write
     * @param offset The start offset of the chunk
     * @param length The length of the bytes to write
     * @throws HTTPWriteException If the write operation fails
     */
    void writeChunk(byte[] data, int offset, int length) throws HTTPWriteException;

    /**
     * Convenience method to read an input stream and send its data in chunks to the client until the end of the stream is reached
     * @param stream The stream to write
     * @throws HTTPWriteException If the write operation fails
     * @throws IOException If an error occurs while reading the stream
     */
    default void writeFromInputStream(InputStream stream) throws HTTPWriteException, IOException {
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
     * @throws HTTPWriteException If the write operation fails
     */
    ResponseToken finalizeResponse() throws HTTPWriteException;
}
