package de.umweltcampus.smallhttp;

import de.umweltcampus.smallhttp.internal.handler.HTTPRequest;
import de.umweltcampus.smallhttp.response.ResponseStartWriter;
import de.umweltcampus.smallhttp.response.ResponseToken;

import java.io.IOException;

public interface ResponseHandler {

    ResponseToken answerRequest(HTTPRequest request, ResponseStartWriter responseWriter) throws IOException;
}
