package de.umweltcampus.smallhttp.internal.handler;

import de.umweltcampus.smallhttp.response.ResponseToken;

public final class ResponseTokenImpl implements ResponseToken {
    /**
     * If true, responses will be tracked.
     * This may help to find bugs where two responses are filled for one request.
     */
    private static final boolean TRACK_RESPONSES = Boolean.getBoolean("smallhttp.trackResponses");
    private static final ThreadLocal<ResponseToken> TOKEN_TRACKER = TRACK_RESPONSES ? new ThreadLocal<>() : null;
    private static final ResponseTokenImpl INSTANCE = new ResponseTokenImpl();

    private ResponseTokenImpl() {}

    static ResponseToken get() {
        if (TRACK_RESPONSES) {
            ResponseToken responseToken = TOKEN_TRACKER.get();
            if (responseToken != null) throw new IllegalStateException("A response has already been tracked!");
            TOKEN_TRACKER.set(INSTANCE);
        }
        return INSTANCE;
    }

    static boolean validate(ResponseToken token) {
        clearTracking(true);
        return token == INSTANCE;
    }

    static void clearTracking(boolean check) {
        if (TRACK_RESPONSES) {
            if (check) {
                ResponseToken responseToken = TOKEN_TRACKER.get();
                if (responseToken == null) throw new IllegalStateException("No response has been tracked yet!");
            }
            TOKEN_TRACKER.set(null);
        }
    }
}
