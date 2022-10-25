package de.umweltcampus.smallhttp.internal.watchdog;

import de.umweltcampus.smallhttp.internal.handler.HTTPClientHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class ClientHandlerTracker {
    private static final Object LOCK = new Object();
    private static final Set<HTTPClientHandler> ALL_HANDLERS = Collections.newSetFromMap(new WeakHashMap<>());

    public static void registerHandler(HTTPClientHandler handler) {
        synchronized (LOCK) {
            ALL_HANDLERS.add(handler);
        }
    }

    public static void deregisterHandler(HTTPClientHandler handler) {
        synchronized (LOCK) {
            ALL_HANDLERS.remove(handler);
        }
    }

    public static void notifyShutdown() throws IOException {
        synchronized (LOCK) {
            for (HTTPClientHandler handler : ALL_HANDLERS) {
                handler.shutdown();
            }
        }
    }
}
