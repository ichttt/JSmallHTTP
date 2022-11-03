package de.umweltcampus.smallhttp.internal.watchdog;

import de.umweltcampus.smallhttp.internal.handler.HTTPClientHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class ClientHandlerTracker {
    private final Object LOCK = new Object();
    private final Set<HTTPClientHandler> ALL_HANDLERS = Collections.newSetFromMap(new WeakHashMap<>());

    public ClientHandlerTracker() {}

    public void registerHandler(HTTPClientHandler handler) {
        synchronized (LOCK) {
            ALL_HANDLERS.add(handler);
        }
    }

    public void deregisterHandler(HTTPClientHandler handler) {
        synchronized (LOCK) {
            ALL_HANDLERS.remove(handler);
        }
    }

    public void notifyShutdown() throws IOException {
        synchronized (LOCK) {
            for (HTTPClientHandler handler : ALL_HANDLERS) {
                handler.shutdown();
            }
        }
    }

    void checkTimeout(int readTimeout, int handleTimeout) {
        synchronized (LOCK) {
            for (HTTPClientHandler handler : ALL_HANDLERS) {
                handler.checkTimeout(readTimeout, handleTimeout);
            }
        }
    }
}
