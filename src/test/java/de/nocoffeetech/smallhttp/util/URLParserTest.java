package de.nocoffeetech.smallhttp.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class URLParserTest {

    @Test
    public void testUrlParsingNoParams() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        String base = "/api/noparam";
        URLParser parser = createParser(base, params, "", 0);
        Assertions.assertEquals(base, parser.parseRequestTarget());
        Assertions.assertEquals(params, parser.parseOrGetQuery());
    }

    @Test
    public void testUrlParsingNoParamsTrailingQuestionmark() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        String base = "/api/noparam?";
        URLParser parser = createParser(base, params, "", 0);
        Assertions.assertEquals(base.substring(0, base.length() - 1), parser.parseRequestTarget());
        Assertions.assertEquals(params, parser.parseOrGetQuery());
    }

    @Test
    public void testUrlParsingNoParamsTrailingQuestionmarkWithFragment() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        String base = "/api/noparam?";
        URLParser parser = createParser(base, params, "#asuzdhk", 0);
        Assertions.assertEquals(base.substring(0, base.length() - 1), parser.parseRequestTarget());
        Assertions.assertEquals(params, parser.parseOrGetQuery());
    }

    @Test
    public void testUrlParsingNoParamsWithFragment() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        String base = "/api/noparam/withFragment";
        URLParser parser = createParser(base, params, "#anchor", 0);
        Assertions.assertEquals(base, parser.parseRequestTarget());
        Assertions.assertEquals(params, parser.parseOrGetQuery());
    }

    @Test
    public void testUrlParsingKeyValueParams() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("login", "true");
        params.put("user", "dieter");
        params.put("lastParam", "1337");
        String base = "/api/simpleparam";
        URLParser parser = createParser(base, params, "", 0);
        Assertions.assertEquals(base, parser.parseRequestTarget());
        Assertions.assertEquals(params, parser.parseOrGetQuery());
    }

    @Test
    public void testUrlParsingKeyValueAndOnlyKeyParams() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("login", "true");
        params.put("user", "dieter");
        params.put("useFastAccessor", null);
        params.put("lastParam", "1337");
        String base = "/api/simpleparam/withnoval";
        URLParser parser = createParser(base, params, "", 0);
        Assertions.assertEquals(base, parser.parseRequestTarget());
        Assertions.assertEquals(params, parser.parseOrGetQuery());
    }

    @Test
    public void testUrlParsingKeyValueAndOnlyKeyParamsWithFragment() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("login", "true");
        params.put("user", "dieter");
        params.put("useFastAccessor", null);
        params.put("lastParam", "1337");
        String base = "/api/simpleparam/withnoval/withfragment";
        URLParser parser = createParser(base, params, "#someFragment", 0);
        Assertions.assertEquals(base, parser.parseRequestTarget());
        Assertions.assertEquals(params, parser.parseOrGetQuery());
    }

    @Test
    public void testUrlParsingKeyValueAndOnlyKeyParamsWithFragmentAndOffset() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("login", "true");
        params.put("user", "dieter");
        params.put("useFastAccessor", null);
        params.put("lastParam", "1337");
        String base = "GET /api/simpleparam/withnoval/withfragment/withoffset";
        URLParser parser = createParser(base, params, "#someFragment", 4);
        Assertions.assertEquals(base.substring(4), parser.parseRequestTarget());
        Assertions.assertEquals(params, parser.parseOrGetQuery());
    }

    @Test
    public void testUrlParsingAndUrlDecodingWithKeyValueAndOnlyKeyParamsWithFragmentAndOffset() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("login", "true");
        params.put("user", "könig");
        params.put("useFastAccessor", null);
        params.put("lastParam", "1337");
        params.put("password", "123=sas&dat=?");
        String base = "GET /api/simpleparam/withnoval/withfragment/withoffset/withurlencoding";
        URLParser parser = createParser(base, params, "#someFragment", 4);
        Assertions.assertEquals(base.substring(4), parser.parseRequestTarget());
        Assertions.assertEquals(params, parser.parseOrGetQuery());
    }

    @Test
    public void testExceptionOnEarlyParseQueryCall() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("login", "true");
        params.put("user", "könig");
        params.put("useFastAccessor", null);
        params.put("lastParam", "1337");
        params.put("password", "123=sas&dat=?");
        String base = "/api/exceptiontest";
        URLParser parser = createParser(base, params, "#someFragment", 0);
        Assertions.assertThrows(IllegalStateException.class, parser::parseOrGetQuery);
    }

    @Test
    public void testQueryCaching() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("login", "true");
        String base = "/api/querytesting";
        URLParser parser = createParser(base, params, "#someFragment", 0);
        Assertions.assertEquals(base, parser.parseRequestTarget());
        Map<String, String> parsedParams = parser.parseOrGetQuery();
        Assertions.assertEquals(params, parsedParams);
        Assertions.assertSame(parsedParams, parser.parseOrGetQuery());
    }

    @Test
    public void testAbsoluteFormParsing() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        String base = "http://www.example.org";
        String target = "/pub/WWW/TheProject.html";
        URLParser parser = createParser(base + target, params, "", 0);
        Assertions.assertEquals(target, parser.parseRequestTarget());
        Assertions.assertEquals(params, parser.parseOrGetQuery());
    }

    @Test
    public void testAbsoluteFormParsingWithParams() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("login", "true");
        params.put("user", "dieter");
        params.put("useFastAccessor", null);
        params.put("lastParam", "1337");
        String base = "http://www.example.org";
        String target = "/pub/WWW/TheProject.html";
        URLParser parser = createParser(base + target, params, "", 0);
        Assertions.assertEquals(target, parser.parseRequestTarget());
        Assertions.assertEquals(params, parser.parseOrGetQuery());
    }

    @Test
    public void testAbsoluteFormParsingWithParamsAndFragment() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("login", "true");
        params.put("user", "könig");
        params.put("useFastAccessor", null);
        params.put("lastParam", "1337");
        String base = "http://www.example.org";
        String target = "/pub/WWW/TheProject.html";
        URLParser parser = createParser(base + target, params, "#dieter", 0);
        Assertions.assertEquals(target, parser.parseRequestTarget());
        Assertions.assertEquals(params, parser.parseOrGetQuery());
    }

    private static URLParser createParser(String base, LinkedHashMap<String, String> params, String post, int offset) {
        StringBuilder finalString = new StringBuilder(base);
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            finalString.append(first ? '?' : '&');
            first = false;
            finalString.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            if (entry.getValue() != null) {
                finalString.append('=');
                finalString.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
        }
        finalString.append(post);
        byte[] bytes = finalString.toString().getBytes(StandardCharsets.US_ASCII);
        return new URLParser(bytes, offset, bytes.length);
    }
}
