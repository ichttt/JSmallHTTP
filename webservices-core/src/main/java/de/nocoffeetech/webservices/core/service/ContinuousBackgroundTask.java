package de.nocoffeetech.webservices.core.service;

public interface ContinuousBackgroundTask extends Runnable {

    String name();

    void shutdownAndTerminate(Thread t);

    default int priority() {
        return 3;
    }
}
