package de.umweltcampus.smallhttp.internal.util;

import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.CommonContentTypes;
import de.umweltcampus.smallhttp.internal.handler.HTTPClientHandler;
import de.umweltcampus.smallhttp.internal.handler.InternalConstants;
import de.umweltcampus.smallhttp.response.HTTPWriteException;
import de.umweltcampus.smallhttp.response.ResponseStartWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HeaderParsingHelper {
    /** Returned when an invalid char in the sequence was found before the searched char */
    private static final int INVALID_CHAR_FOUND = -1;
    /** Returned when the char could not be found and the end was reached */
    private static final int TOO_LARGE = -2;

    private static final byte[] INVALID_CHAR_FOUND_TEXT = "Found invalid character during parsing of header!".getBytes(StandardCharsets.UTF_8);
    private static final byte[] URI_END_REACHED_TEXT = ("Request URI was too large, entire uri must fit into " + InternalConstants.MAX_REQUEST_TARGET_LENGTH + " bytes!").getBytes(StandardCharsets.UTF_8);
    private static final byte[] END_REACHED_TEXT = ("Header was too large, entire header must fit into " + InternalConstants.MAX_HEADER_SIZE_BYTES + " bytes!").getBytes(StandardCharsets.UTF_8);

    public static void handleError(ResponseStartWriter writer, boolean inUri, int errorCode) throws HTTPWriteException {
        if (errorCode == INVALID_CHAR_FOUND) {
            writer.respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN).addHeader(HTTPClientHandler.CONNECTION_CLOSE_HEADER).writeBodyAndFlush(INVALID_CHAR_FOUND_TEXT);
        } else if (errorCode == TOO_LARGE) {
            if (inUri) {
                writer.respond(Status.URI_TOO_LONG, CommonContentTypes.PLAIN).addHeader(HTTPClientHandler.CONNECTION_CLOSE_HEADER).writeBodyAndFlush(URI_END_REACHED_TEXT);
            } else {
                writer.respond(Status.REQUEST_HEADER_FIELDS_TOO_LARGE, CommonContentTypes.PLAIN).addHeader(HTTPClientHandler.CONNECTION_CLOSE_HEADER).writeBodyAndFlush(END_REACHED_TEXT);
            }
        } else {
            throw new RuntimeException("Unknown error code " + errorCode);
        }
    }


    /**
     * Finds the next space character in a given byte array
     * @param toSearch The array to search
     * @param handler The current handler parsing the request
     * @return The index of the space, or one of the error codes defined at the start of the file
     */
    public static int findRequestLineSplit(byte[] toSearch, HTTPClientHandler handler) throws IOException {
        int readInitial = handler.getRead();
        int read = handler.getRead();
        int availableBytes = handler.getAvailableBytes();
        do {
            for (; read < availableBytes; read++) {
                if (read - readInitial > InternalConstants.MAX_REQUEST_TARGET_LENGTH) return TOO_LARGE;

                byte byteAtPos = toSearch[read];
                if (byteAtPos == ' ') {
                    return read;
                } else if (byteAtPos == '\r' || byteAtPos == '\n') {
                    return INVALID_CHAR_FOUND;
                }
            }
            availableBytes = handler.readMoreBytes(toSearch);
        } while (availableBytes > 0);

        return TOO_LARGE;
    }

    /**
     * Finds the next colon character in a given byte array
     * @param toSearch The array to search
     * @param handler The current handler parsing the request
     * @return The index of the colon, or one of the error codes defined at the start of the file
     */
    public static int findHeaderSplit(byte[] toSearch, HTTPClientHandler handler) throws IOException {
        int read = handler.getRead();
        int availableBytes = handler.getAvailableBytes();
        do {
            for (; read < availableBytes; read++) {
                byte byteAtPos = toSearch[read];
                if (byteAtPos == ':') {
                    return read;
                }
                for (char forbiddenChar : InternalConstants.FORBIDDEN_HEADER_NAME_CHARS) {
                    if (byteAtPos == forbiddenChar)
                        return INVALID_CHAR_FOUND;
                }
            }
            availableBytes = handler.readMoreBytes(toSearch);
        } while (availableBytes > 0);
        return TOO_LARGE;
    }

    /**
     * Finds the next CR LF sequence in a given byte array
     * @param toSearch The array to search
     * @param handler The current handler parsing the request
     * @return The index of the LF, or one of the error codes defined at the start of the file
     */
    public static int findHeaderEnd(byte[] toSearch, HTTPClientHandler handler) throws IOException {
        int read = handler.getRead();
        int availableBytes = handler.getAvailableBytes();
        // Max - 1 as we need to find two char, CR and LF
        do {
            for (; read < (availableBytes - 1); read++) {
                byte byteAtPos = toSearch[read];
                if (byteAtPos == '\r') {
                    if (toSearch[read + 1] == '\n') {
                        return read + 1; // plus on as we want to return the \n pos
                    } else {
                        // "rouge" CR is disallowed, see https://www.rfc-editor.org/rfc/rfc9112#section-2.2
                        return INVALID_CHAR_FOUND;
                    }
                }
                for (char forbiddenChar : InternalConstants.FORBIDDEN_HEADER_VALUE_CHARS) {
                    if (forbiddenChar == byteAtPos)
                        return INVALID_CHAR_FOUND;
                }
            }
            availableBytes = handler.readMoreBytes(toSearch);
        } while (availableBytes > 0);
        return TOO_LARGE;
    }
}
