package de.umweltcampus.smallhttp.internal.handler;

import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.CommonContentTypes;
import de.umweltcampus.smallhttp.response.ResponseStartWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

class HeaderParsingHelper {
    /** Returned when an invalid char in the sequence was found before the searched char */
    private static final int INVALID_CHAR_FOUND = -1;
    /** Returned when the char could not be found and the end was reached */
    private static final int END_REACHED = -2;

    private static final byte[] INVALID_CHAR_FOUND_TEXT = "Found invalid character during parsing of header!".getBytes(StandardCharsets.UTF_8);
    private static final byte[] URI_END_REACHED_TEXT = ("Request URI was too large, entire uri must fit into " + InternalConstants.MAX_REQUEST_TARGET_LENGTH + " bytes!").getBytes(StandardCharsets.UTF_8);
    private static final byte[] END_REACHED_TEXT = ("Header was too large, entire header must fit into " + InternalConstants.MAX_HEADER_SIZE_BYTES + " bytes!").getBytes(StandardCharsets.UTF_8);

    static void handleError(ResponseStartWriter writer, boolean inUri, int errorCode) throws IOException {
        if (errorCode == INVALID_CHAR_FOUND) {
            writer.respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN).writeBodyAndFlush(INVALID_CHAR_FOUND_TEXT);
        } else if (errorCode == END_REACHED) {
            if (inUri) {
                writer.respond(Status.URI_TOO_LONG, CommonContentTypes.PLAIN).writeBodyAndFlush(URI_END_REACHED_TEXT);
            } else {
                writer.respond(Status.REQUEST_HEADER_FIELDS_TOO_LARGE, CommonContentTypes.PLAIN).writeBodyAndFlush(END_REACHED_TEXT);
            }
        } else {
            throw new RuntimeException("Unknown error code " + errorCode);
        }
    }


    /**
     * Finds the next space character in a given byte array
     * @param toSearch The array to search
     * @param start The first position in the array to search
     * @param max The last position in the array to search
     * @return The index of the space, or one of the error codes defined at the start of the file
     */
    static int findRequestLineSplit(byte[] toSearch, int start, int max) {
        assert start < max;
        for (int i = start; i < max; i++) {
            byte byteAtPos = toSearch[i];
            if (byteAtPos == ' ') {
                return i;
            } else if (byteAtPos == '\r' || byteAtPos == '\n') {
                return INVALID_CHAR_FOUND;
            }
        }

        return END_REACHED;
    }

    /**
     * Finds the next colon character in a given byte array
     * @param toSearch The array to search
     * @param start The first position in the array to search
     * @param max The last position in the array to search
     * @return The index of the colon, or one of the error codes defined at the start of the file
     */
    static int findHeaderSplit(byte[] toSearch, int start, int max) {
        assert start < max;
        // Max - 1 as we need to find two char, CR and LF
        for (int i = start; i < max; i++) {
            byte byteAtPos = toSearch[i];
            if (byteAtPos == ':') {
                return i;
            }
            for (char forbiddenChar : InternalConstants.FORBIDDEN_HEADER_NAME_CHARS) {
                if (byteAtPos == forbiddenChar)
                    return INVALID_CHAR_FOUND;
            }
        }
        return END_REACHED;
    }

    /**
     * Finds the next CR LF sequence in a given byte array
     * @param toSearch The array to search
     * @param start The first position in the array to search
     * @param max The last position in the array to search
     * @return The index of the LF, or one of the error codes defined at the start of the file
     */
    static int findHeaderEnd(byte[] toSearch, int start, int max) {
        assert start < max;
        // Max - 1 as we need to find two char, CR and LF
        for (int i = start; i < (max - 1); i++) {
            byte byteAtPos = toSearch[i];
            if (byteAtPos == '\r' && toSearch[i + 1] == '\n') {
                return i + 1; // plus on as we want to return the \n pos
            }
            for (char forbiddenChar : InternalConstants.FORBIDDEN_HEADER_VALUE_CHARS) {
                if (forbiddenChar == byteAtPos)
                    return INVALID_CHAR_FOUND;
            }
        }
        return END_REACHED;
    }
}
