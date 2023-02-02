package de.nocoffeetech.smallhttp.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

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

    @Test
    public void testWriteToHeader() throws IOException {
        for (Status status : Status.values()) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            status.writeToHeader(outputStream);
            String resultingString = outputStream.toString(StandardCharsets.US_ASCII);
            Assertions.assertEquals(status.code + " " + status.httpName + "\r\n", resultingString, "Failed at status " + status);
        }
    }
}
