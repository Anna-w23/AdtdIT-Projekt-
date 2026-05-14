package de.spacemate.service.mock;

import de.spacemate.model.Customer;
import de.spacemate.service.NotificationService;

/**
 * Simulates customer communication by printing to console.
 * Replace with a real email/SMS implementation without changing any call sites.
 */
public class MockNotificationService implements NotificationService {

    @Override
    public void sendQuestionnaire(Customer customer) {
        log(customer, "QUESTIONNAIRE sent");
    }

    @Override
    public void sendDocumentList(Customer customer) {
        log(customer, "DOCUMENT LIST sent");
    }

    @Override
    public void sendReminder(Customer customer, String subject) {
        log(customer, "REMINDER [" + subject + "] sent");
    }

    @Override
    public void sendAppointmentConfirmation(Customer customer, String appointmentDetails) {
        log(customer, "APPOINTMENT CONFIRMATION sent: " + appointmentDetails);
    }

    @Override
    public void sendMedicalResults(Customer customer, boolean flightEligible) {
        String outcome = flightEligible ? "APPROVED" : "REJECTED";
        log(customer, "MEDICAL RESULTS sent – outcome: " + outcome);
    }

    private void log(Customer customer, String message) {
        System.out.println("[NOTIFICATION] " + customer.getFullName() + " → " + message);
    }
}