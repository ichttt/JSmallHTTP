package de.umweltcampus.webservices.endpoint;

import de.umweltcampus.smallhttp.base.HTTPRequest;
import de.umweltcampus.smallhttp.data.Method;
import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.CommonContentTypes;
import de.umweltcampus.smallhttp.header.PrecomputedHeaderKey;
import de.umweltcampus.smallhttp.response.HTTPWriteException;
import de.umweltcampus.smallhttp.response.ResponseStartWriter;
import de.umweltcampus.smallhttp.response.ResponseToken;
import de.umweltcampus.webservices.service.WebserviceBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EndpointModule<SRV extends WebserviceBase, T extends BaseEndpoint<SRV>> {
    public static final PrecomputedHeaderKey ALLOWED_HEADER = PrecomputedHeaderKey.create("Allow");
    private static final Logger LOGGER = LogManager.getLogger(EndpointModule.class);
    private final String prefix;
    private final Class<SRV> serviceType;
    private final Map<String, T> endpoints;
    private final List<Validator<SRV, T>> validators;

    public EndpointModule(String prefix, Class<SRV> serviceType, Map<String, T> endpoints, List<Validator<SRV, T>> validators) {
        this.prefix = prefix;
        this.serviceType = serviceType;
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

        for (Validator<SRV, T> validator : validators) {
            ResponseToken result = validator.validate(request, responseStartWriter, request.getPath(), endpoint);
            if (result != null)
                return result;
        }

        try {
            if (request.getMethod() == Method.OPTIONS) {
                return endpoint.handleOptions(request, responseStartWriter);
            }
            return endpoint.answerRequest(request, responseStartWriter);
        } catch (RuntimeException e) {
            LOGGER.warn("Internal Endpoint error", e);
            return responseStartWriter.respond(Status.INTERNAL_SERVER_ERROR, CommonContentTypes.PLAIN).writeBodyAndFlush("Unknown Endpoint failure");
        }
    }

    public void fillContext(WebserviceBase webserviceBase) {
        SRV castedService = this.serviceType.cast(webserviceBase);
        for (T value : this.endpoints.values()) {
            value.setContext(castedService);
        }
    }

    public static class Builder<SRV extends WebserviceBase, T extends BaseEndpoint<SRV>> {
        private final HashMap<String, T> endpoints = new HashMap<>();
        private final ArrayList<Validator<SRV, T>> validators = new ArrayList<>();
        private final String prefix;
        private final Class<SRV> serviceType;

        public Builder(String prefix, Class<SRV> serviceType) {
            this.prefix = prefix;
            this.serviceType = serviceType;
        }

        public Builder<SRV, T> addEndpoint(String uri, T endpoint) {
            this.endpoints.put(uri, endpoint);
            return this;
        }

        public Builder<SRV, T> addValidator(Validator<SRV, T> validator) {
            this.validators.add(validator);
            return this;
        }

        public EndpointModule<SRV, T> build() {
            validators.trimToSize();
            return new EndpointModule<>(prefix, serviceType, endpoints, validators);
        }
    }

    public interface Validator<SRV extends WebserviceBase, T extends BaseEndpoint<SRV>> {
        /**
         * @return Null to indicate the validator is fine with the request, a {@link ResponseToken} if the validator has intervened and send a response to the client
         */
        ResponseToken validate(HTTPRequest request, ResponseStartWriter responseStartWriter, String uri, T endpoint);
    }
}
