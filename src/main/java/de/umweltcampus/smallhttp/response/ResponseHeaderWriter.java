package de.umweltcampus.smallhttp.response;

import java.io.IOException;

/**
 * An interface that allows the application to write header data or continue to the body once it finishes.
 */
public interface ResponseHeaderWriter {

    /**
     * Add custom headers to the request, like Set-Cookie or other relevant headers.
     * <br>
     * Some headers get set by the Server itself, see {@link BuiltinHeaders}.
     * If any of these headers is specified, the server will not set its corresponding builtin header and will instead use the user-supplied value
     * @param name The name of the header. The name must not contain any spaces, new lines or null bytes
     * @param value The value of the header.
     * @return The current header writer to add more headers or start with the body
     */
    ResponseHeaderWriter addHeader(String name, String value);

    /**
     * Resets the entire response builder, allowing the response to be filled again.
     * <br>
     * This should not be used in commonly used code, but is rather intended to be used in case an unexpected exception is thrown.
     * @return A writer to begin with a new response
     */
    ResponseStartWriter resetResponseBuilder();

    /**
     * Ends writing of the header and begins to write the body.
     * Usage of this method is strongly preferred over {@link #beginBodyWithUnknownSize()}.
     * This header writer becomes invalid once this is called.
     * <br>
     * Once this is called, the server might begin writing data to the client. Resetting the writer will not be possible again.
     * Because of this, <strong>any exception thrown by the handler after this call can not be handled properly!</strong>
     * In that case, the connection to the client will be forcibly closed!
     * @param size The size of the body
     * @return The new body writer
     */
    ResponseBodyWriter beginBodyWithKnownSize(int size) throws IOException;

    /**
     * Ends writing of the header and begins to write the body.
     * Use this only if the length of the response can't be determined easily. Otherwise, use {@link #beginBodyWithKnownSize(int)}.
     * This header writer becomes invalid once this is called.
     * <br>
     * Once this is called, the server might begin writing data to the client.
     * Because of this, <strong>any exception thrown by the handler after this call can not be handled properly!</strong>
     * In that case, the connection to the client will be forcibly closed!
     * @return The new body writer
     */
    ResponseBodyWriter beginBodyWithUnknownSize() throws IOException;
}
