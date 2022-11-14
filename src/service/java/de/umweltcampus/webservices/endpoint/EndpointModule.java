package de.umweltcampus.webservices.endpoint;

import de.umweltcampus.smallhttp.data.Method;
import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.CommonContentTypes;
import de.umweltcampus.smallhttp.header.PrecomputedHeaderKey;
import de.umweltcampus.smallhttp.internal.handler.HTTPRequest;
import de.umweltcampus.smallhttp.response.HTTPWriteException;
import de.umweltcampus.smallhttp.response.ResponseStartWriter;
import de.umweltcampus.smallhttp.response.ResponseToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class EndpointModule<T extends BaseEndpoint> {
    public static final PrecomputedHeaderKey ALLOWED_HEADER = new PrecomputedHeaderKey("Allow");
    private static final Logger LOGGER = LogManager.getLogger(EndpointModule.class);
    private final String prefix;
    private final Map<String, T> endpoints;
    private final List<Validator<T>> validators;

    public EndpointModule(String prefix, Map<String, T> endpoints, List<Validator<T>> validators) {
        this.prefix = prefix;
        this.endpoints = endpoints;
        this.validators = validators;
    }

    public boolean handles(HTTPRequest request) {
        return request.getPath().startsWith(prefix);
    }

    public ResponseToken handleRequest(HTTPRequest request, ResponseStartWriter responseStartWriter) throws HTTPWriteException {
        String shortUri = request.getPath().substring(prefix.length());
        T endpoint = endpoints.get(shortUri);

        if (endpoint == null) {
            return responseStartWriter.respond(Status.NOT_FOUND, CommonContentTypes.PLAIN).writeBodyAndFlush("Endpoint not found!");
        }

        Method[] allowedMethods = endpoint.getAllowedMethods();

        if (allowedMethods != null) {
            boolean ok = false;
            for (Method method : allowedMethods) {
                ok |= method == request.getMethod();
            }
            if (!ok) {
                return responseStartWriter.respond(Status.METHOD_NOT_ALLOWED, CommonContentTypes.PLAIN)
                        .addHeader(ALLOWED_HEADER, endpoint.getValidMethodsString())
                        .writeBodyAndFlush("Wrong method, expected one of [", endpoint.getValidMethodsString(), "] but got ", request.getMethod().toString());
            }
        }

        if (request.getContentLength() > endpoint.getMaxBodySizeBytes()) {
            return responseStartWriter.respond(Status.CONTENT_TOO_LARGE, CommonContentTypes.PLAIN).writeBodyAndFlush("Body size too long, max size for this is endpoint is ", Integer.toString(endpoint.getMaxBodySizeBytes()), " bytes");
        }

        for (Validator<T> validator : validators) {
            ResponseToken result = validator.validate(request, responseStartWriter, request.getPath(), endpoint);
            if (result != null)
                return result;
        }

        try {
            if (request.getMethod() == Method.OPTIONS) {
                return endpoint.handleOptions(request, responseStartWriter);
            }
            if (request.getMethod() == Method.HEAD) {
                return endpoint.handleHead(request, responseStartWriter);
            } else {
                return endpoint.answerRequest(request, responseStartWriter);
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Internal Endpoint error", e);
            return responseStartWriter.respond(Status.INTERNAL_SERVER_ERROR, CommonContentTypes.PLAIN).writeBodyAndFlush("Unknown Endpoint failure");
        }
    }

    public static class Builder<T extends BaseEndpoint> {
        private final HashMap<String, T> endpoints = new HashMap<>();
        private final ArrayList<Validator<T>> validators = new ArrayList<>();
        private final String prefix;

        public Builder(String prefix) {
            this.prefix = prefix;
        }

        public Builder<T> addEndpoint(String uri, T endpoint) {
            this.endpoints.put(uri, endpoint);
            return this;
        }

        public Builder<T> addValidator(Validator<T> validator) {
            this.validators.add(validator);
            return this;
        }

        public EndpointModule<T> build() {
            validators.trimToSize();
            return new EndpointModule<>(prefix, endpoints, validators);
        }
    }

    public interface Validator<T extends BaseEndpoint> {
        /**
         * @return Null to indicate the validator is fine with the request, a {@link ResponseToken} if the validator has intervened and send a response to the client
         */
        ResponseToken validate(HTTPRequest request, ResponseStartWriter responseStartWriter, String uri, T endpoint);
    }
}
