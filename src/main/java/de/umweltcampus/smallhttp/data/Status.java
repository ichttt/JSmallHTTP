package de.umweltcampus.smallhttp.data;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

// See https://www.rfc-editor.org/rfc/rfc9110#name-status-codes
public enum Status {
    // 1xx - Informational
    CONTINUE(100), SWITCHING_PROTOCOLS(101),
    // 2xx - Successful
    OK(200), CREATED(201), ACCEPTED(202), NON_AUTHORITATIVE_INFORMATION(203), NO_CONTENT(204),
    RESET_CONTENT(205), PARTIAL_CONTENT(206),
    // 3xx - Redirection
    MULTIPLE_CHOICES(300), MOVED_PERMANENTLY(301), FOUND(302), SEE_OTHER(303), NOT_MODIFIED(304),
    USE_PROXY(305), TEMPORARY_REDIRECT(307), PERMANENT_REDIRECT(308),
    // 4xx - Client Error
    BAD_REQUEST(400), UNAUTHORIZED(401), PAYMENT_REQUIRED(402), FORBIDDEN(403), NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405), NOT_ACCEPTABLE(406), PROXY_AUTHENTICATION_REQUIRED(407), REQUEST_TIMEOUT(408),
    CONFLICT(409), GONE(410), LENGTH_REQUIRED(411), PRECONDITION_FAILED(412), CONTENT_TOO_LARGE(413),
    URI_TOO_LONG(414), UNSUPPORTED_MEDIA_TYPE(415), RANGE_NOT_SATISFIABLE(416), EXPECTATION_FAILED(417),
    MISDIRECTED_REQUEST(421), UNPROCESSABLE_CONTENT(422), UPGRADE_REQUIRED(426),
    // 5xx - Server Error
    INTERNAL_SERVER_ERROR(500), NOT_IMPLEMENTED(501), BAD_GATEWAY(502), SERVICE_TIMEOUT(503),
    GATEWAY_TIMEOUT(504), HTTP_VERSION_NOT_SUPPORTED(505);

    public final int code;
    public final String httpName;
    private final byte[] responseBytes;

    Status(int code) {
        this.code = code;
        String originalName = name();
        // Convert _ to spaces and make lowercase
        String tmpName = originalName.replace('_', ' ').toLowerCase(Locale.ROOT);
        // Capitalize the first letters of the words
        String[] split = tmpName.split(" ");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            String s = split[i];
            assert !s.isEmpty();
            builder.append(s.substring(0, 1).toUpperCase(Locale.ROOT)).append(s.substring(1));
            if (i != split.length - 1) {
                builder.append(' ');
            }
        }
        this.httpName = builder.toString();

        byte[] httpNameBytes = this.httpName.getBytes(StandardCharsets.US_ASCII);
        byte[] codeAsStringBytes = Integer.toString(this.code).getBytes(StandardCharsets.US_ASCII);
        assert codeAsStringBytes.length == 3;
        // version number + plus + version string + CR LF
        this.responseBytes = Arrays.copyOf(codeAsStringBytes, codeAsStringBytes.length + 1 + httpNameBytes.length + 2);
        this.responseBytes[codeAsStringBytes.length] = ' ';
        for (int i = 0; i < httpNameBytes.length; i++) {
            this.responseBytes[i + codeAsStringBytes.length + 1] = httpNameBytes[i];
        }
        this.responseBytes[this.responseBytes.length - 2] = '\r';
        this.responseBytes[this.responseBytes.length - 1] = '\n';
    }

    public void writeToHeader(OutputStream stream) throws IOException {
        stream.write(this.responseBytes);
    }
}
