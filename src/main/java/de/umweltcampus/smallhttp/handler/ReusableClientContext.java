package de.umweltcampus.smallhttp.handler;

import java.util.ArrayList;
import java.util.List;

class ReusableClientContext {
    public final byte[] headerBuffer;
    public final byte[] responseBuffer;

    ReusableClientContext() {
        this.headerBuffer = new byte[InternalConstants.MAX_HEADER_SIZE_BYTES];
        this.responseBuffer = new byte[InternalConstants.MAX_HEADER_SIZE_BYTES];
    }

    void reset() {
    }
}
