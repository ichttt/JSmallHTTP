package de.nocoffeetech.smallhttp.base;

import de.nocoffeetech.smallhttp.response.HTTPWriteException;
import de.nocoffeetech.smallhttp.response.ResponseStartWriter;
import de.nocoffeetech.smallhttp.response.ResponseToken;

public interface RequestHandler {

    /**
     * The main handler responsible for answering a client's HTTP request.
     * This method may be called on different threads for different requests at a time.
     * <br>
     * An implementor MUST always handle a request, except for the case when the writing to the client fails (see {@link HTTPWriteException}
     * @param request The request the client wants to get answered
     * @param responseWriter The writer that can initiate the sending of a response to the client
     * @return A response token that is returned by the writer, signaling the successful handling of the request
     * @throws HTTPWriteException If a write operation of data to the client fails
     */
    ResponseToken answerRequest(HTTPRequest request, ResponseStartWriter responseWriter) throws HTTPWriteException;
}
