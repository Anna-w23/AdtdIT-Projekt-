package de.spacemate.service;

import de.spacemate.model.CustomerResponse;

import java.util.UUID;

public interface CustomerCommunicationService {
    CustomerResponse sendQuestionnaire(UUID customerId);
    void sendAppointmentProposal(UUID customerId, UUID appointmentId);
    CustomerResponse collectAppointmentResponse(UUID customerId, UUID appointmentId);
    boolean hasPendingResponse(UUID appointmentId);
    void sendAppointmentReminder(UUID customerId, UUID appointmentId);
    CustomerResponse sendIndemnityAgreement(UUID customerId);
    CustomerResponse collectIndemnityResponse(UUID customerId);
}
