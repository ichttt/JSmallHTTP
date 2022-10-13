package de.umweltcampus.smallhttp.handler;

class ReusableClientContext {
    public final byte[] headerBuffer;

    ReusableClientContext() {
        this.headerBuffer = new byte[InternalConstants.MAX_HEADER_SIZE_BYTES];
    }
}
