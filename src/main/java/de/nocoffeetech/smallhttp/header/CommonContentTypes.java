package de.nocoffeetech.smallhttp.header;

// Some common MIME content types, see https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
public enum CommonContentTypes {
    PLAIN("text/plain", true),
    HTML("text/html", true),
    JSON("application/json", false),
    XML("application/xml", false),
    JS("text/javascript", true),
    CSS("text/css", true),
    PNG("image/png", false),
    JPEG("image/jpg", false),
    GIF("image/gif", false),
    BINARY_DATA("application/octet-stream", false);

    public final PrecomputedHeader header;
    public final String mimeType;

    CommonContentTypes(String name, boolean utf8) {
        this.mimeType = name;
        this.header = PrecomputedHeader.create(BuiltinHeaders.CONTENT_TYPE.headerKey, name + (utf8 ? ";charset=UTF-8" : ""));
    }
}
