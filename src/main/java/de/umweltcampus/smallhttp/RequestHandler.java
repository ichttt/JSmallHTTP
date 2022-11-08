package de.umweltcampus.smallhttp;

import de.umweltcampus.smallhttp.internal.handler.HTTPRequest;
import de.umweltcampus.smallhttp.response.HTTPWriteException;
import de.umweltcampus.smallhttp.response.ResponseStartWriter;
import de.umweltcampus.smallhttp.response.ResponseToken;

public interface RequestHandler {

    /**
     * The main handler responsible for answering a client's HTTP request.
     * This method may be called on different threads for different requests at a time.
     * <br>
     * An implementor MUST always a request, except for the case when the writing to the client fails (see {@link HTTPWriteException}
     * @param request The request the client wants to get answered
     * @param responseWriter The writer that can initiate the sending of a response to the client
     * @return A response token that is returned by the writer, signaling the successful handling of the request
     * @throws HTTPWriteException If a write operation of data to the client fails
     */
    ResponseToken answerRequest(HTTPRequest request, ResponseStartWriter responseWriter) throws HTTPWriteException;
}
