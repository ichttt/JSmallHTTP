package de.umweltcampus.smallhttp;

import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.CommonContentTypes;

import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Test2 {

    public static void main(String[] args) throws Exception {
        HTTPServer server = new HTTPServer(8888, (request, responseWriter) -> responseWriter.respond(Status.OK, CommonContentTypes.PLAIN).writeBodyAndFlush("ok"));
        Thread.sleep(500);
        Thread requesterThread = new Thread(Test2::request);
        requesterThread.start();

        Thread.sleep(1000);
        server.shutdown(true);
        System.out.println("Shut down!");
    }

    private static void request() {
        try {
            Socket socket = new Socket("localhost", 8888);
            socket.getOutputStream().write("GET /sana_d".getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            System.out.println("First");
            Thread.sleep(2000);
            socket.getOutputStream().write("dasd HTTP/1.1\r\n".getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            System.out.println("Second");
            Thread.sleep(2000);
            socket.getOutputStream().write("Connection: close\r\nHost: localhost\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            System.out.println("Third");
            System.out.println(new String(socket.getInputStream().readAllBytes(), StandardCharsets.US_ASCII));
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
