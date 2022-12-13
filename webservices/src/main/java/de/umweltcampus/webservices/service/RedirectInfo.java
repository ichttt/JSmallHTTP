package de.umweltcampus.webservices.service;

import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.PrecomputedHeader;

/**
 * Represents an HTTP redirect, which can either be permanent or temporary
 */
public class RedirectInfo {
    public final String from;
    public final String to;
    public final PrecomputedHeader locationHeader;
    public final Status codeToUse;

    /**
     * Creates a new HTTP redirect, sending clients requesting the path specified in "from" to the path specified in "to".
     * @param from The request target that should be redirected
     * @param to The target path that represents the new location
     * @param codeToUse The HTTP code to use, should be a 3xx code
     */
    public RedirectInfo(String from, String to, Status codeToUse) {
        if (from.equals(to)) throw new IllegalArgumentException("From equals to!");
        this.from = from;
        this.to = to;
        this.locationHeader = PrecomputedHeader.create("Location", to);
        this.codeToUse = codeToUse;
    }

    /**
     * Creates a new HTTP redirect, sending clients requesting the path specified in "from" to the path specified in "to" using the 307 Temporary Redirect status code
     * @param from The request target that should be redirected
     * @param to The target path that represents the new location
     */
    public static RedirectInfo newTempRedirect(String from, String to) {
        return new RedirectInfo(from, to, Status.TEMPORARY_REDIRECT);
    }

    /**
     * Creates a new HTTP redirect, sending clients requesting the path specified in "from" to the path specified in "to" using the 308 Permanent Redirect status code
     * @param from The request target that should be redirected
     * @param to The target path that represents the new location
     */
    public static RedirectInfo newPermanentRedirect(String from, String to) {
        return new RedirectInfo(from, to, Status.PERMANENT_REDIRECT);
    }
}
