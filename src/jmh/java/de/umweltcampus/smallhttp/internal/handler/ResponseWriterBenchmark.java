package de.umweltcampus.smallhttp.internal.handler;

import de.umweltcampus.smallhttp.data.HTTPVersion;
import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.CommonContentTypes;
import de.umweltcampus.smallhttp.header.PrecomputedHeader;
import de.umweltcampus.smallhttp.header.PrecomputedHeaderKey;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Thread)
public class ResponseWriterBenchmark {
    private static final PrecomputedHeader STATIC_HEADER = new PrecomputedHeader(new PrecomputedHeaderKey("Testabc"), "Valueabc");
    private static final PrecomputedHeaderKey DYNAMIC_HEADER = new PrecomputedHeaderKey("Dynamicabc");
    private ReusableClientContext context;
    private String headerTestString;
    private String string1;
    private String string2;
    private String string3;
    private String string4;

    @Setup
    public void setup() {
        context = new ReusableClientContext();
        headerTestString = "StringToTestDynamicHeaderValues";
        string1 = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna ";
        string2 = "aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata ";
        string3 = "sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet ";
        string4 = "clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.";
    }

    @Benchmark
    public void testHeaderPerformance(Blackhole blackhole) throws Exception {
        try (BlackholeOutputStream outputStream = new BlackholeOutputStream(blackhole)) {
            ResponseWriter writer = new ResponseWriter(outputStream, null, context, HTTPVersion.HTTP_1_1);
            writer.respond(Status.OK, CommonContentTypes.PLAIN)
                    .addHeader(STATIC_HEADER)
                    .addHeader(DYNAMIC_HEADER, headerTestString)
                    .sendWithoutBody();
        }
    }

    @Benchmark
    public void testContentSendingPerformance(Blackhole blackhole) throws Exception {
        try (BlackholeOutputStream outputStream = new BlackholeOutputStream(blackhole)) {
            ResponseWriter writer = new ResponseWriter(outputStream, null, context, HTTPVersion.HTTP_1_1);
            writer.respond(Status.OK, CommonContentTypes.PLAIN)
                    .writeBodyAndFlush(string1, string2, string3, string4);
        }
    }
}
