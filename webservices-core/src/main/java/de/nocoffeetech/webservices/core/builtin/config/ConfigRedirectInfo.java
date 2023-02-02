package de.nocoffeetech.webservices.core.builtin.config;

public class ConfigRedirectInfo {
    public final String from;
    public final String to;
    public final boolean permanent;

    public ConfigRedirectInfo(String from, String to, boolean permanent) {
        this.from = from;
        this.to = to;
        this.permanent = permanent;
    }
}
