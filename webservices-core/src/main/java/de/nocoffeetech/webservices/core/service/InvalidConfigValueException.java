package de.nocoffeetech.webservices.core.service;

/**
 * Signals that a config value is invalid and that the service can thus not be created
 */
public class InvalidConfigValueException extends Exception {
    private final String optionName;
    private final String detailMessage;

    public InvalidConfigValueException(String optionName, String detailMessage) {
        super("The config value " + optionName + " is invalid: " + detailMessage);
        this.optionName = optionName;
        this.detailMessage = detailMessage;
    }

    public String getOptionName() {
        return optionName;
    }

    public String getDetailMessage() {
        return detailMessage;
    }
}
