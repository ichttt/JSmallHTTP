package de.umweltcampus.smallhttp.response;

import de.umweltcampus.smallhttp.data.Status;

/**
 * An interface that serves as the entry point to sending a response to a client.
 */
public interface ResponseStartWriter {

    /**
     * Begins a response by setting the response status.
     * @param status The response status of the client.
     * @return The new header writer.
     */
    ResponseHeaderWriter respond(Status status);
}
