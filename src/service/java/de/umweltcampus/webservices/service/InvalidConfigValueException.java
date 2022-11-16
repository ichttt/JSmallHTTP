package de.umweltcampus.webservices.service;

/**
 * Signals that a config value is invalid and that the service can thus not be created
 */
public class InvalidConfigValueException extends Exception {
    private final String optionName;
    private final String message;

    public InvalidConfigValueException(String optionName, String message) {
        super("The config value " + optionName + " is invalid: " + message);
        this.optionName = optionName;
        this.message = message;
    }
}
