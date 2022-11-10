package de.umweltcampus.webservices.endpoint;


import de.umweltcampus.smallhttp.data.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class EndpointLoggingHelper {
    private static final Logger LOGGER = LogManager.getLogger(EndpointLoggingHelper.class);

    private static final Marker CLIENT_ERROR_MARKER = MarkerManager.getMarker("HTTP_CLIENT_ERROR");
    private static final Marker SERVER_ERROR_MARKER = MarkerManager.getMarker("HTTP_SERVER_ERROR");
    private static final Marker OTHER_ERROR_MARKER = MarkerManager.getMarker("HTTP_ERROR");

    static void logStatusCode(Class<? extends BaseEndpoint> causingEndpoint, String message, Status status) {
        String endpointName = causingEndpoint.getName();
        LOGGER.warn(getMarkerForStatus(status), "Endpoint {} failed with status code {} {}: Message: '{}'", endpointName, status.code, status, message);
    }

    static void logStatusCodeWithCause(Class<? extends BaseEndpoint> causingEndpoint, String message, Status status, Throwable cause) {
        String endpointName = causingEndpoint.getName();
        LOGGER.warn(getMarkerForStatus(status), "Endpoint {} failed with status {} {}: Message: '{}'", endpointName, status.code, status, message, cause);
    }

    private static Marker getMarkerForStatus(Status status) {
        if (status.code >= 400 && status.code < 500) {
            return CLIENT_ERROR_MARKER;
        } else if (status.code >= 500 && status.code < 600) {
            return SERVER_ERROR_MARKER;
        } else {
            return OTHER_ERROR_MARKER;
        }
    }
}
