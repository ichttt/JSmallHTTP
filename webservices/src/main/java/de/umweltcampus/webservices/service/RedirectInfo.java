package de.umweltcampus.webservices.service;

import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.PrecomputedHeader;

public class RedirectInfo {
    public final String from;
    public final String to;
    public final PrecomputedHeader locationHeader;
    public final Status codeToUse;

    public RedirectInfo(String from, String to, Status codeToUse) {
        this.from = from;
        this.to = to;
        this.locationHeader = PrecomputedHeader.create("Location", to);
        this.codeToUse = codeToUse;
    }

    public static RedirectInfo newTempRedirect(String from, String to) {
        return new RedirectInfo(from, to, Status.TEMPORARY_REDIRECT);
    }

    public static RedirectInfo newPermanentRedirect(String from, String to) {
        return new RedirectInfo(from, to, Status.PERMANENT_REDIRECT);
    }
}
