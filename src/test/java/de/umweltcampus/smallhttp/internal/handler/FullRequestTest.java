package de.umweltcampus.smallhttp.internal.handler;

import de.umweltcampus.smallhttp.HTTPServer;
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

public class FullRequestTest {
    private static HTTPServer server;

    @BeforeAll
    public static void setup() throws IOException {
        server = new HTTPServer(6549, (request, responseWriter) -> {
            if (request.getMethod() == Method.GET) return responseWriter.respond(Status.OK, CommonContentTypes.PLAIN).writeBodyAndFlush("Passt");
            if (request.getMethod() == Method.POST) return responseWriter.respond(Status.OK, CommonContentTypes.PLAIN).writeBodyAndFlush("Super");
            return responseWriter.respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN).writeBodyAndFlush("Ja ne kein plan");
        });
    }

    @AfterAll
    public static void shutdown() throws IOException {
        server.shutdown(true);
    }

    private static final String TO_SEND = "GET / HTTP/1.1\r\nConnection: close\r\nUser-Agent: Test\r\nHost:nocoffee.tech\r\n\r\n";
    private static final String TO_SEND_KEEP_ALIVE = "POST / HTTP/1.1\r\nUser-Agent: Test\r\nHost:nocoffee.tech\r\n\r\n";

    @Test
    public void sendHttpRequestInDifferentChunks() throws Exception {
        Assertions.assertTimeoutPreemptively(Duration.of(30, ChronoUnit.SECONDS), () -> {
                    for (int chunkSize = 1; chunkSize < TO_SEND.length() - 1; chunkSize++) {
                        Socket socket = new Socket("localhost", 6549);
                        socket.setSoTimeout(50);
                        for (int i = 0; i < TO_SEND.length(); i += chunkSize) {
                            socket.getOutputStream().write(TO_SEND.substring(i, Math.min(i + chunkSize, TO_SEND.length())).getBytes(StandardCharsets.US_ASCII));
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
                socket.setSoTimeout(50);
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
}
