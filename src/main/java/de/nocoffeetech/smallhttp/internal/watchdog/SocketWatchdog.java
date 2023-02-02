package de.nocoffeetech.smallhttp.internal.watchdog;

import de.nocoffeetech.smallhttp.base.HTTPServer;

public class SocketWatchdog implements Runnable {
    private final int totalReadTimeout;
    private final int totalWriteTimeout;
    private final ClientHandlerTracker tracker;
    private final HTTPServer server;

    public SocketWatchdog(int totalReadTimeout, int totalWriteTimeout, ClientHandlerTracker tracker, HTTPServer server) {
        assert totalWriteTimeout != -1 || totalReadTimeout != -1;
        this.totalReadTimeout = totalReadTimeout;
        this.totalWriteTimeout = totalWriteTimeout;
        this.tracker = tracker;
        this.server = server;
    }

    @Override
    public void run() {
        int sleepTime;
        if (this.totalReadTimeout == -1)
            sleepTime = this.totalWriteTimeout;
        else if (this.totalWriteTimeout == -1)
            sleepTime = this.totalReadTimeout;
        else
            sleepTime = Math.min(this.totalReadTimeout, totalWriteTimeout);

        if (sleepTime < 5000)
            sleepTime = 5000;
        while (!server.isShutdown()) {
            tracker.checkTimeout(totalReadTimeout, totalWriteTimeout);
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                // ignore -
            }
        }
    }
}
