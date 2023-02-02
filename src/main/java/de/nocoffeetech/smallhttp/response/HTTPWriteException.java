package de.nocoffeetech.smallhttp.response;

import java.io.IOException;

/**
 * An exception that signalizes that an error occurred while writing a HTTP response
 */
public class HTTPWriteException extends Exception {

    public HTTPWriteException(IOException source) {
        super(source);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
