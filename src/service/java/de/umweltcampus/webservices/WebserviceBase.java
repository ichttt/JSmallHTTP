package de.umweltcampus.webservices;

import de.umweltcampus.smallhttp.RequestHandler;
import de.umweltcampus.smallhttp.internal.handler.HTTPRequest;
import de.umweltcampus.smallhttp.response.HTTPWriteException;
import de.umweltcampus.smallhttp.response.ResponseStartWriter;
import de.umweltcampus.smallhttp.response.ResponseToken;

public abstract class WebserviceBase implements RequestHandler {

    @Override
    public ResponseToken answerRequest(HTTPRequest request, ResponseStartWriter responseWriter) throws HTTPWriteException {
        throw new RuntimeException("TODO");
    }
}
