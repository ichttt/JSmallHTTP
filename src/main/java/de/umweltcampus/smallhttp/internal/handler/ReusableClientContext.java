package de.umweltcampus.smallhttp.internal.handler;

import java.time.Clock;

class ReusableClientContext {
    public final byte[] headerBuffer;
    public final byte[] responseBuffer;
    public final Clock clock;

    ReusableClientContext() {
        this(Clock.systemUTC());
    }

    ReusableClientContext(Clock clock) {
        this.headerBuffer = new byte[InternalConstants.MAX_HEADER_SIZE_BYTES];
        this.responseBuffer = new byte[InternalConstants.MAX_HEADER_SIZE_BYTES];
        this.clock = clock;
    }

    void reset() {
    }
}
