package de.nocoffeetech.webservices.core.endpoint;

import de.nocoffeetech.smallhttp.data.Status;
import de.nocoffeetech.smallhttp.response.HTTPWriteException;
import de.nocoffeetech.smallhttp.response.ResponseStartWriter;
import de.nocoffeetech.smallhttp.response.ResponseToken;

/**
 * Helper interface that can be implemented by endpoints that satisfy all the following criteria most of the time:
 * The endpoint
 * <ul>
 *     <li>Answers with OK most of the time</li>
 *     <li>Uses the same static mime time most of the time</li>
 *     <li>Only writes a single string most of the time</li>
 * </ul>
 */
public interface SimpleEndpoint {

    default ResponseToken simpleResponse(ResponseStartWriter writer, String text) throws HTTPWriteException {
        return writer.respond(Status.OK, getDefaultMimeType()).writeBodyAndFlush(text);
    }

    String getDefaultMimeType();
}
