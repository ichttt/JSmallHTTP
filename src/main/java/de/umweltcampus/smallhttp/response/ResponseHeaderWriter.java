package de.umweltcampus.smallhttp.response;

import de.umweltcampus.smallhttp.header.BuiltinHeaders;
import de.umweltcampus.smallhttp.header.PrecomputedHeader;
import de.umweltcampus.smallhttp.header.PrecomputedHeaderKey;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * An interface that allows the application to write header data or continue to the body once it finishes.
 */
public interface ResponseHeaderWriter {

    /**
     * Add custom headers to the request, like Set-Cookie or other relevant headers.
     * <br>
     * If the value of the header is a constant, use {@link #addHeader(PrecomputedHeader)} instead.
     * <br>
     * Some headers get set by the Server itself, see {@link BuiltinHeaders}. These should not be set by hand.
     * @param name The name of the header. The precomputed header key should be computed once (like in a static final field)
     * @param value The value of the header
     * @return The current header writer to add more headers or start with the body
     */
    ResponseHeaderWriter addHeader(PrecomputedHeaderKey name, String value);

    /**
     * Add custom headers to the request, like Set-Cookie or other relevant headers.
     * <br>
     * If the value of the header is <strong>NOT</strong> a constant, use {@link #addHeader(PrecomputedHeaderKey, String)} instead.
     * <br>
     * Some headers get set by the Server itself, see {@link BuiltinHeaders}. These should not be set by hand.
     * @param header The complete header. The precomputed header should be computed once (like in a static final field)
     * @return The current header writer to add more headers or start with the body
     */
    ResponseHeaderWriter addHeader(PrecomputedHeader header);

    /**
     * Resets the entire response builder, allowing the response to be filled again.
     * <br>
     * This should not be used in commonly used code, but is rather intended to be used in case an unexpected exception is thrown.
     * @return A writer to begin with a new response
     */
    ResponseStartWriter resetResponseBuilder();

    /**
     * Ends writing of the headers and begins to write the body.
     * Usage of this method is strongly preferred over {@link #beginBodyWithUnknownSize()}.
     * This header writer becomes invalid once this is called.
     * <br>
     * Once this is called, the server might begin writing data to the client. Resetting the writer will not be possible again.
     * Because of this, <strong>any exception thrown by the handler after this call can not be handled properly!</strong>
     * In that case, the connection to the client will be forcibly closed!
     * @param size The size of the body
     * @return The new body writer
     * @throws HTTPWriteException If the write operation fails
     */
    FixedResponseBodyWriter beginBodyWithKnownSize(long size) throws HTTPWriteException;

    /**
     * Ends writing of the headers and begins to write the body.
     * Use this only if the length of the response can't be determined easily. Otherwise, use {@link #beginBodyWithKnownSize(long)}.
     * This header writer becomes invalid once this is called.
     * <br>
     * <strong>This method cannot be called for {@link de.umweltcampus.smallhttp.data.HTTPVersion#HTTP_1_0} connections!</strong>
     * HTTP/1.0 does not support proper chunked encoding, and therefor cannot utilize this.
     * <br>
     * Once this is called, the server might begin writing data to the client.
     * Because of this, <strong>any exception thrown by the handler after this call can not be handled properly!</strong>
     * In that case, the connection to the client will be forcibly closed!
     * @return The new body writer
     * @throws HTTPWriteException If the write operation fails
     */
    ChunkedResponseWriter beginBodyWithUnknownSize() throws HTTPWriteException;

    /**
     * Sends the response to the client without any body
     * @throws HTTPWriteException If the write operation fails
     * @return A response token that must be returned, marking the response as complete
     */
    ResponseToken sendWithoutBody() throws HTTPWriteException;

    /**
     * Ends writing of headers and writes a string to the message body and flushes the output to the client.
     * Convenience method to avoid {@link #beginBodyWithKnownSize(long)} followed by {@link FixedResponseBodyWriter#getRawOutputStream()} and {@link FixedResponseBodyWriter#finalizeResponse()}
     * This header writer becomes invalid once this is called.
     * @param s The string to write
     * @return A response token that must be returned, marking the response as complete
     * @throws HTTPWriteException If the write operation fails
     */
    default ResponseToken writeBodyAndFlush(String s) throws HTTPWriteException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        FixedResponseBodyWriter responseBodyWriter = beginBodyWithKnownSize(bytes.length);
        try {
            responseBodyWriter.getRawOutputStream().write(bytes);
        } catch (IOException e) {
            throw new HTTPWriteException(e);
        }
        return responseBodyWriter.finalizeResponse();
    }

    /**
     * Ends writing of headers and writes an array of strings to the message body and flushes the output to the client.
     * Convenience method to avoid {@link #beginBodyWithKnownSize(long)} followed by {@link FixedResponseBodyWriter#getRawOutputStream()} and {@link FixedResponseBodyWriter#finalizeResponse()}
     * This header writer becomes invalid once this is called.
     * @param strings The strings to write
     * @return A response token that must be returned, marking the response as complete
     * @throws HTTPWriteException If the write operation fails
     */
    default ResponseToken writeBodyAndFlush(String... strings) throws HTTPWriteException {
        byte[][] arrays = new byte[strings.length][];
        long length = 0;
        for (int i = 0; i < strings.length; i++) {
            byte[] bytes = strings[i].getBytes(StandardCharsets.UTF_8);
            length += bytes.length;
            arrays[i] = bytes;
        }
        FixedResponseBodyWriter responseBodyWriter = beginBodyWithKnownSize(length);
        OutputStream rawOutputStream = responseBodyWriter.getRawOutputStream();
        for (byte[] array : arrays) {
            try {
                rawOutputStream.write(array);
            } catch (IOException e) {
                throw new HTTPWriteException(e);
            }
        }
        return responseBodyWriter.finalizeResponse();
    }

    /**
     * Ends writing of headers and writes a byte array to the message body and flushes the output to the client.
     * Convenience method to avoid {@link #beginBodyWithKnownSize(long)} followed by {@link FixedResponseBodyWriter#getRawOutputStream()} and {@link FixedResponseBodyWriter#finalizeResponse()}
     * This header writer becomes invalid once this is called.
     * @param bytes The bytes to write
     * @return A response token that must be returned, marking the response as complete
     * @throws HTTPWriteException If the write operation fails
     */
    default ResponseToken writeBodyAndFlush(byte[] bytes) throws HTTPWriteException {
        FixedResponseBodyWriter responseBodyWriter = beginBodyWithKnownSize(bytes.length);
        try {
            responseBodyWriter.getRawOutputStream().write(bytes);
        } catch (IOException e) {
            throw new HTTPWriteException(e);
        }
        return responseBodyWriter.finalizeResponse();
    }
}
