package de.umweltcampus.smallhttp.response;

import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.CommonContentTypes;

/**
 * An interface that serves as the entry point to sending a response to a client.
 */
public interface ResponseStartWriter {

    /**
     * Begins a response by setting the response status.
     * @param status The response status of the client.
     * @return The new header writer.
     */
    ResponseHeaderWriter respond(Status status, String contentType);

    /**
     * Begins a response by setting the response status.
     * @param status The response status of the client.
     * @return The new header writer.
     */
    ResponseHeaderWriter respond(Status status, CommonContentTypes contentType);

    /**
     * Begins a response by setting the response status, omitting any content type.
     * Only use this if the content type is not applicable
     *
     * @param status The response status of the client.
     * @return The new header writer.
     */
    ResponseHeaderWriter respondWithoutContentType(Status status);
}
