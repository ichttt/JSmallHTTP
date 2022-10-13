package de.umweltcampus.smallhttp;

class ReusableClientContext {
    public byte[] headerBuffer;

    ReusableClientContext() {
        this.headerBuffer = new byte[InternalConstants.MAX_HEADER_SIZE_BYTES];
    }
}
