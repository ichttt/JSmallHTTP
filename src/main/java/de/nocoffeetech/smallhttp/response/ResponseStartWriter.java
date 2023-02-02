package de.nocoffeetech.smallhttp.response;

import de.nocoffeetech.smallhttp.data.Status;
import de.nocoffeetech.smallhttp.header.CommonContentTypes;

/**
 * An interface that serves as the entry point to sending a response to a client.
 */
public interface ResponseStartWriter extends ResettableWriter {

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
