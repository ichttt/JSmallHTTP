package de.umweltcampus.smallhttp.response;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An interface that allows the application to write body data using a raw output stream or using convenience functions.
 */
public interface ResponseBodyWriter {

    /**
     * Returns the raw underlying output stream. Any data written to this stream might be sent immediately to the client.
     * @return The raw output stream
     */
    OutputStream getRawOutputStream();

    /**
     * Writes a simple string to the underlying output stream.
     * If called multiple times, the messages will be concatenated.
     * @param msg The message to write
     */
    void writeString(String msg) throws IOException;

    /**
     * Finalizes the response and flushes it to the client.
     * This is also implicitly called once the application finishes its processRequest method.
     * Use this by hand when the response is complete but the code still needs some processing time or wants to make sure that the client got the response before doing something else
     */
    void finalizeResponse() throws IOException;
}
