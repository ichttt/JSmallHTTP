package de.umweltcampus.smallhttp.header;

import de.umweltcampus.smallhttp.util.StringUtil;

public enum BuiltinHeaders {
    // Handled by sendHeader
    SERVER,
    // Handled by beginBodyWithKnownSize
    CONTENT_LENGTH,
    // Handled by respond
    CONTENT_TYPE,
    // Handled by sendHeader
    DATE,
    // Handled by beginBodyWithUnknownSize
    TRANSFER_ENCODING;

    public final PrecomputedHeaderKey headerKey;
    public final String httpName;

    BuiltinHeaders() {
        this.httpName = StringUtil.capitalize('-', name().replace('_', '-'));
        this.headerKey = PrecomputedHeaderKey.create(httpName);
    }
}
