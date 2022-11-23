package de.umweltcampus.smallhttp.internal.handler;

import de.umweltcampus.smallhttp.base.HTTPServer;
import de.umweltcampus.smallhttp.base.HTTPServerBuilder;
import de.umweltcampus.smallhttp.base.RequestHandler;
import de.umweltcampus.smallhttp.data.Method;
import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.CommonContentTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;

public class FullRequestTest {
    private static HTTPServer server;

    @BeforeAll
    public static void setup() throws IOException {
        RequestHandler handler = (request, responseWriter) -> {
            if (request.getMethod() == Method.GET) return responseWriter.respond(Status.OK, CommonContentTypes.PLAIN).writeBodyAndFlush("Passt");
            if (request.getMethod() == Method.POST) return responseWriter.respond(Status.OK, CommonContentTypes.PLAIN).writeBodyAndFlush("Super");
            return responseWriter.respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN).writeBodyAndFlush("Ja ne kein plan");
        };
        server = HTTPServerBuilder.create(6549, handler).build();
    }

    @AfterAll
    public static void shutdown() throws IOException {
        server.shutdown(true);
    }

    private static final String TO_SEND_CLOSE = "GET / HTTP/1.1\r\nConnection: close\r\nUser-Agent: Test\r\nHost:nocoffee.tech\r\n\r\n";
    private static final String TO_SEND_KEEP_ALIVE = "POST / HTTP/1.1\r\nUser-Agent: Test\r\nHost:nocoffee.tech\r\n\r\n";

    @Test
    public void sendHttpRequestInDifferentChunks() throws Exception {
        Assertions.assertTimeoutPreemptively(Duration.of(30, ChronoUnit.SECONDS), () -> {
                    for (int chunkSize = 1; chunkSize < TO_SEND_CLOSE.length() - 1; chunkSize++) {
                        Socket socket = new Socket("localhost", 6549);
                        socket.setSoTimeout(200);
                        for (int i = 0; i < TO_SEND_CLOSE.length(); i += chunkSize) {
                            socket.getOutputStream().write(TO_SEND_CLOSE.substring(i, Math.min(i + chunkSize, TO_SEND_CLOSE.length())).getBytes(StandardCharsets.US_ASCII));
                            socket.getOutputStream().flush();
                            Thread.sleep((int) (Math.random() * 25));
                        }
                        socket.shutdownOutput();
                        byte[] bytes = socket.getInputStream().readAllBytes();
                        String asString = new String(bytes, StandardCharsets.US_ASCII);
                        Assertions.assertEquals("HTTP/1.1 200 Ok", asString.split("\r")[0], "Failed at split " + chunkSize);
                        Assertions.assertTrue(asString.endsWith("Passt"), "Failed at split " + chunkSize + ", reponse is: " + asString);
                        socket.close();
                    }
                });
    }

    @Test
    public void sendHttpRequestInDifferentChunksWithKeepalive() throws Exception {
        Assertions.assertTimeoutPreemptively(Duration.of(30, ChronoUnit.SECONDS), () -> {
            try (Socket socket = new Socket("localhost", 6549)) {
                socket.setSoTimeout(200);
                for (int chunkSize = 1; chunkSize < TO_SEND_KEEP_ALIVE.length() - 1; chunkSize++) {
                    for (int i = 0; i < TO_SEND_KEEP_ALIVE.length(); i += chunkSize) {
                        socket.getOutputStream().write(TO_SEND_KEEP_ALIVE.substring(i, Math.min(i + chunkSize, TO_SEND_KEEP_ALIVE.length())).getBytes(StandardCharsets.US_ASCII));
                        socket.getOutputStream().flush();
                        Thread.sleep((int) (Math.random() * 25));
                    }
                    // Can't readAllBytes as connection stays open. So a bit of hack: Read until we time out
                    // A proper client has much more complex logic, but that would be out of scope for this test
                    byte[] bytes = new byte[4096];
                    int available = 0;
                    int lastRead = 0;
                    do {
                        available += lastRead;
                        try {
                            lastRead = socket.getInputStream().read(bytes, available, bytes.length - available);
                        } catch (SocketTimeoutException e) {
                            break; // most likely just no more data
                        }
                    } while (lastRead != -1);
                    String asString = new String(bytes, 0, available, StandardCharsets.US_ASCII);
                    Assertions.assertEquals("HTTP/1.1 200 Ok", asString.split("\r")[0], "Failed at split " + chunkSize);
                    Assertions.assertTrue(asString.endsWith("Super"), "Failed at split " + chunkSize + ", reponse is: " + asString);
                }
            }
        });
    }

    @Test
    public void testShutdownDuringRead() throws Exception {
        HTTPServer secondaryServer = HTTPServerBuilder.create(8342, (request, responseWriter) -> {
            throw new RuntimeException("Should not get here - request should get cancelled!");
        }).build();
        Thread serverShutdownThread = new Thread(() -> {
            try {
                secondaryServer.shutdown(true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        AtomicReference<Throwable> reference = new AtomicReference<>();
        serverShutdownThread.setUncaughtExceptionHandler((t, e) -> reference.set(e));
        Assertions.assertTimeoutPreemptively(Duration.of(10, ChronoUnit.SECONDS), () -> {
            int chunkSize = 10;
            Socket socket = new Socket("localhost", 8342);
            socket.setSoTimeout(200);
            for (int i = 0; i < TO_SEND_CLOSE.length(); i += chunkSize) {
                if (i == chunkSize) {
                    serverShutdownThread.start();
                }
                socket.getOutputStream().write(TO_SEND_CLOSE.substring(i, Math.min(i + chunkSize, TO_SEND_CLOSE.length())).getBytes(StandardCharsets.US_ASCII));
                socket.getOutputStream().flush();
                Thread.sleep(200);
            }
            socket.shutdownOutput();
            byte[] bytes = socket.getInputStream().readAllBytes();
            String asString = new String(bytes, StandardCharsets.US_ASCII);
            Assertions.assertEquals("HTTP/1.1 500 Internal Server Error", asString.split("\r")[0]);
            Assertions.assertTrue(asString.endsWith("Server closed"), "String ends with " + asString.substring(asString.length() - 25));
            socket.close();
        });
        Assertions.assertTrue(secondaryServer.isShutdown());
        Assertions.assertFalse(serverShutdownThread.isAlive());
        Assertions.assertNull(reference.get());
    }
}
