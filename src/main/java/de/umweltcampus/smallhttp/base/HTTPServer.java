package de.umweltcampus.smallhttp.base;

import de.umweltcampus.smallhttp.internal.handler.HTTPClientHandler;
import de.umweltcampus.smallhttp.internal.watchdog.ClientHandlerTracker;
import de.umweltcampus.smallhttp.internal.watchdog.SocketWatchdog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A server that listens for incoming HTTP requests. Create new instances using {@link HTTPServerBuilder}
 */
public class HTTPServer {
    private final int port;
    private final ServerSocketChannel mainSocket;
    private final ExecutorService executor;
    private final Thread mainSocketListener;
    private final Thread socketWatchdogThread;
    private final ErrorHandler errorHandler;
    private final RequestHandler handler;
    private final int socketTimeout;
    private final boolean allowTraceConnect;
    private final boolean builtinServerWideOptions;
    private final int maxBodyLengthBytes;
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);
    private final ClientHandlerTracker tracker = new ClientHandlerTracker();
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    /**
     * Creates and starts a new HTTP Server with the specified options
     *
     * @throws IOException If the startup of the server failed
     */
    HTTPServer(HTTPServerBuilder builder) throws IOException {
        this.port = builder.getPort();
        this.errorHandler = builder.getErrorHandler();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(true);
        serverSocketChannel.bind(new InetSocketAddress(port));
        this.mainSocket = serverSocketChannel;
        this.executor = new ThreadPoolExecutor(0, builder.getThreadCount(), 5, TimeUnit.MINUTES, new SynchronousQueue<>(), r -> new Thread(r, "HTTPServer port " + port + " client handler " + threadNumber.getAndIncrement()));
        this.handler = builder.getHandler();
        this.socketTimeout = builder.getSocketTimeoutMillis();
        this.allowTraceConnect = builder.isAllowTraceConnect();
        this.builtinServerWideOptions = builder.isBuiltinServerWideOptions();
        this.maxBodyLengthBytes = ((int) builder.getMaxClientBodyLengthKB()) * 1024;

        this.mainSocketListener = new Thread(this::listen);
        this.mainSocketListener.setName("HTTPServer port " + port + " listener");
        this.mainSocketListener.setUncaughtExceptionHandler((t, e) -> this.errorHandler.onListenerInternalException(this, e));
        this.mainSocketListener.start();

        int readTimeout = builder.getRequestHeaderReadTimeoutMillis();
        int handlingTimeout = builder.getRequestHandlingTimeoutMillis();
        if (readTimeout != -1 || handlingTimeout != -1) {
            this.socketWatchdogThread = new Thread(new SocketWatchdog(readTimeout, handlingTimeout, this.tracker, this));
            this.socketWatchdogThread.setPriority(Thread.MIN_PRIORITY);
            this.socketWatchdogThread.setDaemon(true);
            this.socketWatchdogThread.setName("HTTPServer port " + port + " watchdog");
            this.socketWatchdogThread.setUncaughtExceptionHandler((t, e) -> this.errorHandler.onWatchdogInternalException(this, e));
            this.socketWatchdogThread.start();
        } else {
            this.socketWatchdogThread = null;
        }
    }

    private void listen() {
        try {
            while (mainSocket.isOpen()) {
                Socket acceptedSocket = mainSocket.accept().socket();
                if (socketTimeout != -1) {
                    acceptedSocket.setSoTimeout(socketTimeout);
                }
                HTTPClientHandler httpClientHandler = new HTTPClientHandler(acceptedSocket, this.errorHandler, this.handler, this.tracker, this.allowTraceConnect, this.builtinServerWideOptions, this.maxBodyLengthBytes);
                try {
                    this.executor.submit(httpClientHandler);
                } catch (RejectedExecutionException e) {
                    this.errorHandler.onNoAvailableThreadForConnection(this, acceptedSocket, e);
                }
            }
        } catch (IOException e) {
            this.errorHandler.onListenerInternalException(this, e);
        }
    }

    public void shutdown(boolean awaitDeath) throws IOException {
        // Check if a shutdown sequence is already in progress
        if (this.isShutdown.getAndSet(true)) return;
        this.tracker.notifyShutdown();
        if (this.socketWatchdogThread != null) this.socketWatchdogThread.interrupt();

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
