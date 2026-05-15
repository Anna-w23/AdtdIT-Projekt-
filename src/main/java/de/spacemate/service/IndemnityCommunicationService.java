package de.spacemate.service;

import de.spacemate.model.CustomerResponse;

import java.util.UUID;

public interface IndemnityCommunicationService {
    CustomerResponse sendIndemnityAgreement(UUID customerId);
    CustomerResponse collectIndemnityResponse(UUID customerId);
}
