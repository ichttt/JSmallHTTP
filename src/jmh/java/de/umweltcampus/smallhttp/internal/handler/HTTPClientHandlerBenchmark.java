package de.umweltcampus.smallhttp.internal.handler;

import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.internal.watchdog.ClientHandlerTracker;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@State(Scope.Thread)
public class HTTPClientHandlerBenchmark {
    private byte[] toRead;

    @Setup
    public void setup() {
        // Test request on firefox (linux) - modified to close connection
        String example =
"""
GET /test/test.html HTTP/1.1
Host: localhost:8080
User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:106.0) Gecko/20100101 Firefox/106.0
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8
Accept-Language: en-US,en;q=0.5
Accept-Encoding: gzip, deflate, br
DNT: 1
Connection: close
Upgrade-Insecure-Requests: 1
Sec-Fetch-Dest: document
Sec-Fetch-Mode: navigate
Sec-Fetch-Site: none
Sec-Fetch-User: ?1
Custom: 123
Custom:423\040
Custom: 555\040

""".replace("\n", "\r\n");
        toRead = example.getBytes(StandardCharsets.US_ASCII);
    }

    @Benchmark
    public void benchmarkClientHandler(Blackhole blackhole) {
        AtomicBoolean hit = new AtomicBoolean(false);
        HTTPClientHandler handler = new BenchmarkClientHandler(DefaultErrorHandler.INSTANCE, (request, responseWriter) -> {
            blackhole.consume(request);
            blackhole.consume(responseWriter);
            hit.setPlain(true);
            return responseWriter.respondWithoutContentType(Status.NO_CONTENT).sendWithoutBody();
        }, new ClientHandlerTracker(), toRead, blackhole);
        handler.run();
        if (!hit.getPlain()) {
            throw new RuntimeException();
        }
    }
}
