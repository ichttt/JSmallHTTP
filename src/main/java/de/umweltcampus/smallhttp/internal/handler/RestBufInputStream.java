package de.umweltcampus.smallhttp.internal.handler;

import java.io.IOException;
import java.io.InputStream;

public class RestBufInputStream extends InputStream {
    private final byte[] buf;
    private final int readEnd;
    private final InputStream restStream;
    private final int restStreamRead;
    private int bufIndex;
    private int readStream;

    public RestBufInputStream(byte[] buf, int readStart, int readEnd, InputStream restStream, int totalContentLength) {
        this.buf = buf;
        this.bufIndex = readStart;
        this.readEnd = readEnd;
        this.restStream = restStream;
        this.restStreamRead = totalContentLength - (readEnd - readStart);
        this.readStream = 0;
    }

    boolean isDrained() {
        return readStream >= restStreamRead;
    }

    @Override
    public int available() throws IOException {
        if (readEnd > bufIndex) {
            return readEnd - bufIndex;
        } else if (restStreamRead > readStream) {
            return Math.min(restStream.available(), restStreamRead - readStream);
        } else {
            return 0;
        }
    }

    @Override
    public int read() throws IOException {
        if (readEnd > bufIndex) {
            byte b = buf[bufIndex];
            bufIndex++;
            return Byte.toUnsignedInt(b);
        } else if (restStreamRead > readStream) {
            int read = this.restStream.read();
            readStream++;
            return read;
        } else {
            return -1;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }
        if (readEnd > bufIndex) {
            int lengthToCopy = Math.min(readEnd - bufIndex, len);
            System.arraycopy(buf, bufIndex, b, off, lengthToCopy);
            bufIndex += lengthToCopy;
            return lengthToCopy;
        } else if (restStreamRead > readStream) {
            int lengthToRead = Math.min(restStreamRead - readStream, len);
            int actualRead = restStream.read(b, off, lengthToRead);
            if (actualRead != -1)
                readStream += actualRead;
            else
                readStream = restStreamRead;
            return actualRead;
        }
        return -1;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        int len = (int) Math.min(n, Integer.MAX_VALUE);
        if (readEnd > bufIndex) {
            int lengthToSkip = Math.min(readEnd - bufIndex, len);
            bufIndex += lengthToSkip;
            return lengthToSkip;
        } else if (restStreamRead > readStream) {
            int lengthToSkip = Math.min(restStreamRead - readStream, len);
            long actualSkip = restStream.skip(lengthToSkip);
            readStream += actualSkip;
            return actualSkip;
        }
        return 0;
    }
}
