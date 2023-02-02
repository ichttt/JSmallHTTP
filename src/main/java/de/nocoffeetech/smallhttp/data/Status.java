package de.nocoffeetech.smallhttp.data;

import de.nocoffeetech.smallhttp.util.StringUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

// See https://www.rfc-editor.org/rfc/rfc9110#name-status-codes and https://www.iana.org/assignments/http-status-codes/http-status-codes.xhtml
public enum Status {
    // 1xx - Informational
    CONTINUE(100), SWITCHING_PROTOCOLS(101), PROCESSING(102), EARLY_HINTS(103),
    // 2xx - Successful
    OK(200), CREATED(201), ACCEPTED(202), NON_AUTHORITATIVE_INFORMATION(203), NO_CONTENT(204),
    RESET_CONTENT(205), PARTIAL_CONTENT(206), MULTI_STATUS(207), ALREADY_REPORTED(208),
    // 3xx - Redirection
    MULTIPLE_CHOICES(300), MOVED_PERMANENTLY(301), FOUND(302), SEE_OTHER(303), NOT_MODIFIED(304),
    USE_PROXY(305), TEMPORARY_REDIRECT(307), PERMANENT_REDIRECT(308),
    // 4xx - Client Error
    BAD_REQUEST(400), UNAUTHORIZED(401), PAYMENT_REQUIRED(402), FORBIDDEN(403), NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405), NOT_ACCEPTABLE(406), PROXY_AUTHENTICATION_REQUIRED(407), REQUEST_TIMEOUT(408),
    CONFLICT(409), GONE(410), LENGTH_REQUIRED(411), PRECONDITION_FAILED(412), CONTENT_TOO_LARGE(413),
    URI_TOO_LONG(414), UNSUPPORTED_MEDIA_TYPE(415), RANGE_NOT_SATISFIABLE(416), EXPECTATION_FAILED(417),
    MISDIRECTED_REQUEST(421), UNPROCESSABLE_CONTENT(422), LOCKED(423), FAILED_DEPENDENCY(424),
    TOO_EARLY(425), UPGRADE_REQUIRED(426), PRECONDITION_REQUIRED(428), TOO_MANY_REQUEST(429),
    REQUEST_HEADER_FIELDS_TOO_LARGE(431), UNAVAILABLE_FOR_LEGAL_REASONS(451),
    // 5xx - Server Error
    INTERNAL_SERVER_ERROR(500), NOT_IMPLEMENTED(501), BAD_GATEWAY(502), SERVICE_TIMEOUT(503),
    GATEWAY_TIMEOUT(504), HTTP_VERSION_NOT_SUPPORTED(505), VARIANT_ALSO_NEGOTIATES(506),
    INSUFFICIENT_STORAGE(507), LOOP_DETECTED(508), NETWORK_AUTHENTICATION_REQUIRED(511);

    public final int code;
    public final String httpName;
    private final byte[] responseBytes;

    Status(int code) {
        this.code = code;
        String originalName = name();
        // Convert _ to spaces and make lowercase
        String tmpName = originalName.replace('_', ' ');
        this.httpName = StringUtil.capitalize(' ', tmpName);

        // Build the string together
        String tmpString = this.code + " " + httpName + "\r\n";
        // And cache the resulting ascii bytes
        this.responseBytes = tmpString.getBytes(StandardCharsets.US_ASCII);
    }

    public void writeToHeader(OutputStream stream) throws IOException {
        stream.write(this.responseBytes);
    }
}
