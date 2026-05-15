package de.spacemate.repository;

import de.spacemate.model.IndemnityAgreement;

import java.util.Optional;
import java.util.UUID;

public interface IndemnityAgreementRepository {
    void save(IndemnityAgreement agreement);
    Optional<IndemnityAgreement> findById(UUID agreementId);
    Optional<IndemnityAgreement> findByCustomerId(UUID customerId);
}
