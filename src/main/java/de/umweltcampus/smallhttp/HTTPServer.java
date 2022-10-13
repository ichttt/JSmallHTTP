package de.umweltcampus.smallhttp;

import de.umweltcampus.smallhttp.handler.HTTPClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class HTTPServer {
    private final int port;
    private final ServerSocket mainSocket;
    private final ExecutorService executor;
    private final Thread mainSocketListener;

    /**
     * Creates and starts a new HTTP Server on the specified port
     * @param port The port to listen on
     * @throws IOException If the startup of the server failed
     */
    public HTTPServer(int port) throws IOException {
        // Use 4 times the available hardware processors as max threads, as not all threads may run at once
        // (some may hang during read operations)
        this(port, Runtime.getRuntime().availableProcessors() * 4);
    }

    /**
     * Creates and starts a new HTTP Server on the specified port
     * @param port The port to listen on
     * @param maxThreads The maximum number of threads to handle requests on
     * @throws IOException If the startup of the server failed
     */
    public HTTPServer(int port, int maxThreads) throws IOException {
        this.port = port;
        this.mainSocket = new ServerSocket(port);
        this.executor = new ThreadPoolExecutor(0, maxThreads, 5, TimeUnit.MINUTES, new SynchronousQueue<>());

        this.mainSocketListener = new Thread(this::listen);
        this.mainSocketListener.setName("HTTPServer port " + port + " listener");
        this.mainSocketListener.setUncaughtExceptionHandler((t, e) -> {
            //TODO log
            try {
                this.shutdown();
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
                this.executor.submit(new HTTPClientHandler(acceptedSocket));
            }
        } catch (IOException e) {
            // TODO log
            try {
                this.shutdown();
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    public void shutdown() throws IOException {
        //TODO improve
        this.executor.shutdownNow();
        this.mainSocket.close();
        try {
            this.mainSocketListener.wait();
        } catch (InterruptedException e) {
            // huh, ok then lets not wait
        }
    }

    public int getPort() {
        return port;
    }
}
