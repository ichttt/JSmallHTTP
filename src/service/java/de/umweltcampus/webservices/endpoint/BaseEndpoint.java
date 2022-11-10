package de.umweltcampus.webservices.endpoint;

import de.umweltcampus.smallhttp.data.Method;
import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.CommonContentTypes;
import de.umweltcampus.smallhttp.internal.handler.HTTPRequest;
import de.umweltcampus.smallhttp.response.HTTPWriteException;
import de.umweltcampus.smallhttp.response.ResponseStartWriter;
import de.umweltcampus.smallhttp.response.ResponseToken;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class BaseEndpoint {
    private final int maxBodySizeBytes;
    private final Method[] allowedMethods;
    private final String validMethodsString;

    /**
     * Creates a new base endpoint
     * @param maxBodySizeBytes The maximum number of bytes that this endpoint is willing to handle
     * @param allowedMethods The allowed methods. Keep empty for all
     */
    public BaseEndpoint(int maxBodySizeBytes, Method... allowedMethods) {
        // validate methods
        Set<Method> seenMethods = new HashSet<>();
        for (Method allowedMethod : allowedMethods) {
            if (allowedMethod == null) throw new IllegalArgumentException("Found null method!");
            if (!seenMethods.add(allowedMethod)) throw new IllegalArgumentException("Duplicate method " + allowedMethod);
        }

        this.maxBodySizeBytes = maxBodySizeBytes;
        this.allowedMethods = allowedMethods.length == 0 ? null : allowedMethods;
        this.validMethodsString = Arrays.stream(allowedMethods).map(Enum::name).collect(Collectors.joining(", "));
    }

    public final int getMaxBodySizeBytes() {
        return this.maxBodySizeBytes;
    }

    public final Method[] getAllowedMethods() {
        return this.allowedMethods;
    }

    String getValidMethodsString() {
        return this.validMethodsString;
    }

    public abstract ResponseToken answerRequest(HTTPRequest request, ResponseStartWriter responseWriter) throws HTTPWriteException;

    public ResponseToken handleOptions(HTTPRequest request, ResponseStartWriter responseStartWriter) throws HTTPWriteException {
        return responseStartWriter.respondWithoutContentType(Status.NO_CONTENT).addHeader(EndpointModule.ALLOWED_HEADER, getValidMethodsString()).sendWithoutBody();
    }

    protected final ResponseToken responseError(ResponseStartWriter responseWriter, String msg, Status status) throws HTTPWriteException {
        EndpointLoggingHelper.logStatusCode(this.getClass(), msg, status);
        return responseWriter.respond(status, CommonContentTypes.PLAIN).writeBodyAndFlush(msg);
    }

    protected final ResponseToken responseError(ResponseStartWriter responseWriter, String msg, Status status, Exception cause) throws HTTPWriteException {
        EndpointLoggingHelper.logStatusCodeWithCause(this.getClass(), msg, status, cause);
        return responseWriter.respond(status, CommonContentTypes.PLAIN).writeBodyAndFlush(msg);
    }
}
