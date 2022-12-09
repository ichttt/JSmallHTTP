package de.umweltcampus.webservices.endpoint;

import de.umweltcampus.smallhttp.base.HTTPRequest;
import de.umweltcampus.smallhttp.data.Method;
import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.CommonContentTypes;
import de.umweltcampus.smallhttp.response.HTTPWriteException;
import de.umweltcampus.smallhttp.response.ResponseStartWriter;
import de.umweltcampus.smallhttp.response.ResponseToken;
import de.umweltcampus.smallhttp.util.URLParser;
import de.umweltcampus.webservices.service.WebserviceBase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base class representing an endpoint that dynamically answers requests
 */
public abstract class BaseEndpoint<T extends WebserviceBase> {
    private final Method[] allowedMethods;
    private final String validMethodsString;
    private T context;

    /**
     * Creates a new base endpoint
     * @param allowedMethods The allowed methods. Keep empty for all
     */
    public BaseEndpoint(Method... allowedMethods) {
        // validate methods
        Set<Method> seenMethods = new HashSet<>();
        for (Method allowedMethod : allowedMethods) {
            if (allowedMethod == null) throw new IllegalArgumentException("Found null method!");
            if (allowedMethod == Method.OPTIONS) throw new IllegalArgumentException("HEAD and OPTIONS must not be in the allow list, they are handled in methods other then answerRequest!");
            if (!seenMethods.add(allowedMethod)) throw new IllegalArgumentException("Duplicate method " + allowedMethod);
        }

        this.allowedMethods = allowedMethods.length == 0 ? null : allowedMethods;
        this.validMethodsString = Arrays.stream(allowedMethods).map(Enum::name).collect(Collectors.joining(", "));
    }

    // Getters
    public final Method[] getAllowedMethods() {
        return this.allowedMethods;
    }

    final String getValidMethodsString() {
        return this.validMethodsString;
    }

    // Main serving methods
    public abstract ResponseToken answerRequest(HTTPRequest request, ResponseStartWriter responseWriter) throws HTTPWriteException;

    // Overrideable optional methods

    public ResponseToken handleOptions(HTTPRequest request, ResponseStartWriter responseStartWriter) throws HTTPWriteException {
        return responseStartWriter.respondWithoutContentType(Status.NO_CONTENT).addHeader(EndpointModule.ALLOWED_HEADER, getValidMethodsString()).sendWithoutBody();
    }

    public int getMaxBodySizeBytes() {
        return Short.MAX_VALUE;
    }

    // Helper for error responses
    protected final ResponseToken responseError(ResponseStartWriter responseWriter, String msg, Status status) throws HTTPWriteException {
        EndpointLoggingHelper.logStatusCode(this.getClass(), msg, status);
        return responseWriter.respond(status, CommonContentTypes.PLAIN).writeBodyAndFlush(msg);
    }

    protected final ResponseToken responseError(ResponseStartWriter responseWriter, String msg, Status status, Exception cause) throws HTTPWriteException {
        EndpointLoggingHelper.logStatusCodeWithCause(this.getClass(), msg, status, cause);
        return responseWriter.respond(status, CommonContentTypes.PLAIN).writeBodyAndFlush(msg);
    }

    // Optional helper methods

    protected final Map<String, String> parseUrlencodedBody(HTTPRequest request) throws IOException {
        // https://url.spec.whatwg.org/#application/x-www-form-urlencoded - decode with UTF-8
        String s = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return URLParser.parseQuery(s);
    }

    // Context methods
    protected T getContext() {
        return context;
    }

    public void setContext(T context) {
        if (this.context != null) throw new IllegalStateException();
        if (context == null) throw new IllegalArgumentException();
        this.context = context;
    }
}
