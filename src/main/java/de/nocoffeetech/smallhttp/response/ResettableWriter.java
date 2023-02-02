package de.nocoffeetech.smallhttp.response;

public interface ResettableWriter {

    /**
     * Resets the entire response builder, allowing the response to be filled again.
     * <br>
     * This should not be used in commonly used code, but is rather intended to be used in case an unexpected exception is thrown.
     * @return A writer to begin with a new response
     */
    ResponseStartWriter resetResponseBuilder();

    /**
     * Indicates if the writer can still be reset to the original state.
     * @return true if the writer can still be reset, false otherwise
     */
    boolean canResetResponseWriter();
}
