package de.umweltcampus.smallhttp;

import de.umweltcampus.smallhttp.internal.handler.HTTPClientHandler;
import de.umweltcampus.smallhttp.internal.watchdog.ClientHandlerTracker;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class HTTPServer {
    private final int port;
    private final ServerSocket mainSocket;
    private final ExecutorService executor;
    private final Thread mainSocketListener;
    private final ResponseHandler handler;
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    /**
     * Creates and starts a new HTTP Server on the specified port
     * @param port The port to listen on
     * @throws IOException If the startup of the server failed
     */
    public HTTPServer(int port, ResponseHandler handler) throws IOException {
        // Use 8 times the available hardware processors as max threads, as not all threads may run at once
        // (some may hang during read operations or are keep alive connections)
        this(port, Math.min(256, Runtime.getRuntime().availableProcessors() * 8), handler);
    }

    /**
     * Creates and starts a new HTTP Server on the specified port
     * @param port The port to listen on
     * @param maxThreads The maximum number of threads to handle requests on
     * @throws IOException If the startup of the server failed
     */
    public HTTPServer(int port, int maxThreads, ResponseHandler handler) throws IOException {
        this.port = port;
        this.mainSocket = new ServerSocket(port);
        this.executor = new ThreadPoolExecutor(0, maxThreads, 5, TimeUnit.MINUTES, new SynchronousQueue<>());
        this.handler = handler;

        this.mainSocketListener = new Thread(this::listen);
        this.mainSocketListener.setName("HTTPServer port " + port + " listener");
        this.mainSocketListener.setUncaughtExceptionHandler((t, e) -> {
            //TODO log
            try {
                this.shutdown(false);
            } catch (IOException ex) {
                // ignore
            }
        });
        this.mainSocketListener.start();
    }

    private void listen() {
        try {
            while (!mainSocket.isClosed()) {
                Socket acceptedSocket = mainSocket.accept();
                HTTPClientHandler httpClientHandler = new HTTPClientHandler(acceptedSocket, this.handler);
                try {
                    this.executor.submit(httpClientHandler);
                } catch (RejectedExecutionException e) {
                    try {
                        acceptedSocket.close(); // TODO handle better
                    } catch (IOException ignored) {}
                }
            }
        } catch (IOException e) {
            // TODO log
            try {
                this.shutdown(false);
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    public void shutdown(boolean awaitDeath) throws IOException {
        // Check if a shutdown sequence is already in progress
        if (this.isShutdown.getAndSet(true)) return;
        ClientHandlerTracker.notifyShutdown();

        this.mainSocket.close();
        this.executor.shutdown();
        if (awaitDeath) {
            try {
                // The main thread should be finished pretty quickly, as it just accepts new requests
                this.mainSocketListener.join(1000);
            } catch (InterruptedException e) {
                // huh, ok then lets not wait
            }
            if (this.mainSocketListener.isAlive()) {
                throw new IOException("Failed to shutdown main socket listener!");
            }
            boolean terminated = false;
            try {
                terminated = this.executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // huh, ok then lets not wait
            }
            if (!terminated) {
                throw new IOException("Failed to shutdown socket handlers!");
            }
        }
    }

    public boolean isShutdown() {
        return isShutdown.get();
    }

    public int getPort() {
        return port;
    }
}
