package de.spacemate.service;

import de.spacemate.model.Customer;

/**
 * Sends messages to customers (questionnaires, reminders, documents, results).
 * The mock implementation logs to console; a real implementation would send email/SMS.
 */
public interface NotificationService {
    void sendQuestionnaire(Customer customer);
    void sendDocumentList(Customer customer);
    void sendReminder(Customer customer, String subject);
    void sendAppointmentConfirmation(Customer customer, String appointmentDetails);
    void sendMedicalResults(Customer customer, boolean flightEligible);
}
