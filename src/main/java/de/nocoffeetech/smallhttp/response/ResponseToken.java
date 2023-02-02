package de.nocoffeetech.smallhttp.response;

/**
 * An empty interface/object that marks that is returned once a request is completed.
 * This is used to aid the control flow to make sure that a request always gets a response.
 * Implementation must not create a custom implementation of that class, but return the original token provided by the response writer.
 */
public interface ResponseToken {}
