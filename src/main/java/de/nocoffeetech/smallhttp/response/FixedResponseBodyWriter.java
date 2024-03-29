package de.nocoffeetech.smallhttp.response;

import java.io.OutputStream;
import java.nio.channels.SocketChannel;

/**
 * An interface that allows the application to write body data using a raw output stream or using convenience functions.
 */
public interface FixedResponseBodyWriter {

    /**
     * Returns the raw underlying output stream. Any data written to this stream might be sent immediately to the client.
     * @return The raw output stream
     */
    OutputStream getRawOutputStream();

    /**
     * Returns the raw underlying socket channel. Any data written to this stream might be sent immediately to the client.
     * @return The raw output stream
     */
    SocketChannel getRawSocketChannel();

    /**
     * Finalizes the response and flushes it to the client.
     * This must be called when the response is completed.
     * @return A response token that must be returned, marking the response as complete
     * @throws HTTPWriteException If the write operation fails
     */
    ResponseToken finalizeResponse() throws HTTPWriteException;
}
