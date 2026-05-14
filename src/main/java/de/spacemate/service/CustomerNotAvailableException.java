package de.spacemate.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Thrown when the PM tries to schedule an appointment at a time the customer
 * did not declare as available in their questionnaire.
 */
public class CustomerNotAvailableException extends RuntimeException {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("EEE dd MMM HH:mm");

    public CustomerNotAvailableException(String customerName, LocalDateTime requestedTime) {
        super(customerName + " is not available at " + requestedTime.format(FMT)
                + ". Choose a slot that matches their declared availability.");
    }
}
