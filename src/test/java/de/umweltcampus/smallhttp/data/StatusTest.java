package de.umweltcampus.smallhttp.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StatusTest {

    @Test
    public void checkStatusCodes() {
        Status[] allStatusCodes = Status.values();
        for (Status toVerify : allStatusCodes) {
            for (int i = 0; i < toVerify.ordinal(); i++) {
                Status otherStatus = allStatusCodes[i];
                Assertions.assertTrue(toVerify.code > otherStatus.code, "Found a status (" + otherStatus + ") that has a higher or equal code to the status " + toVerify + ", which should not happen if the class is filled correctly!");
            }
        }
    }

    @Test
    public void checkNames() {
        for (Status allStatusCode : Status.values()) {
            String nameWithSpaces = allStatusCode.name().replace('_', ' ');
            Assertions.assertTrue(allStatusCode.httpName.equalsIgnoreCase(nameWithSpaces), "Name for " + allStatusCode + " is invalid!");
        }
    }
}
