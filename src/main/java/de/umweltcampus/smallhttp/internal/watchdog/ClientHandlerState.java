package de.umweltcampus.smallhttp.internal.watchdog;

import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandlerState {
    private static final int WAITING_FOR_REQUEST_START = 0;
    private static final int READING_REQUEST = 1;
    private static final int HANDLING_REQUEST = 2;
    private static final int SHUTDOWN_REQUESTED = 3;

    private final AtomicInteger currentState = new AtomicInteger(WAITING_FOR_REQUEST_START);
    private volatile long lastTransitionTimestamp = -1;

    /**
     * Transitions into the request reading state
     * @return True if the transition was successful, false if the current handler should be shutdown.
     */
    public boolean startReadingRequest() {
        lastTransitionTimestamp = System.currentTimeMillis();
        int prevValue = currentState.compareAndExchange(WAITING_FOR_REQUEST_START, READING_REQUEST);
        if (prevValue == WAITING_FOR_REQUEST_START) {
            return true;
        } else if (prevValue == SHUTDOWN_REQUESTED) {
            return false;
        } else {
            throw error("READING_REQUEST", READING_REQUEST, WAITING_FOR_REQUEST_START, prevValue);
        }
    }

    /**
     * Transitions into the request handling state
     * @return True if the transition was successful, false if the current handler should be shutdown.
     */
    public boolean startHandlingRequest() {
        lastTransitionTimestamp = System.currentTimeMillis();
        int prevValue = currentState.compareAndExchange(READING_REQUEST, HANDLING_REQUEST);
        if (prevValue == READING_REQUEST) {
            return true;
        } else if (prevValue == SHUTDOWN_REQUESTED) {
            return false;
        } else {
            throw error("HANDLING_REQUEST", HANDLING_REQUEST, READING_REQUEST, prevValue);
        }
    }

    /**
     * Transitions into waiting for next request state
     * @return True if the transition was successful, false if the current handler should be shutdown.
     */
    public boolean startAwaitingNextRequest() {
        lastTransitionTimestamp = System.currentTimeMillis();
        int prevValue = currentState.compareAndExchange(HANDLING_REQUEST, WAITING_FOR_REQUEST_START);
        if (prevValue == HANDLING_REQUEST) {
            return true;
        } else if (prevValue == SHUTDOWN_REQUESTED) {
            return false;
        } else {
            throw error("WAITING_FOR_REQUEST_START", WAITING_FOR_REQUEST_START, HANDLING_REQUEST, prevValue);
        }
    }

    public boolean startShutdown() {
        lastTransitionTimestamp = System.currentTimeMillis();
        int prevValue = currentState.getAndSet(SHUTDOWN_REQUESTED);
        return prevValue == WAITING_FOR_REQUEST_START;
    }

    public boolean isTimedOut(int readTimeout, int handleTimeout) {
        int currentState = this.currentState.get();
        if (currentState == SHUTDOWN_REQUESTED || currentState == WAITING_FOR_REQUEST_START) return false;
        // Local copy of volatile variable
        long lastTransitionTimestamp = this.lastTransitionTimestamp;
        if (lastTransitionTimestamp == -1) return false;
        long currentTime = System.currentTimeMillis();
        if (currentState == READING_REQUEST) return currentTime > lastTransitionTimestamp + readTimeout;
        else if (currentState == HANDLING_REQUEST) return currentTime > lastTransitionTimestamp + handleTimeout;
        else throw new RuntimeException("Unknown state " + currentState);
    }

    private static IllegalStateException error(String target, int targetCode, int expectedCode, int actualCode) {
        return new IllegalStateException("Unexpected state while trying to transition to " + target + "(" + targetCode + ")." +
                "Value should have been " + expectedCode + " or " + SHUTDOWN_REQUESTED + ", but was " + actualCode);
    }
}
