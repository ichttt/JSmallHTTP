package de.umweltcampus.smallhttp.internal.handler;

import de.umweltcampus.smallhttp.internal.util.ResponseDateFormatter;

class ReusableClientContext {
    public final byte[] headerBuffer;
    public final byte[] responseBuffer;
    public final ResponseDateFormatter responseDateFormatter;

    ReusableClientContext() {
        this(new ResponseDateFormatter());
    }

    ReusableClientContext(ResponseDateFormatter formatter) {
        this.headerBuffer = new byte[InternalConstants.MAX_HEADER_SIZE_BYTES];
        this.responseBuffer = new byte[InternalConstants.MAX_HEADER_SIZE_BYTES];
        this.responseDateFormatter = formatter;
    }

    void reset() {
    }
}
