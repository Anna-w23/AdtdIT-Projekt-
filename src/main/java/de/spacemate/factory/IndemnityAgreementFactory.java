package de.spacemate.factory;

import de.spacemate.model.Customer;
import de.spacemate.model.IndemnityAgreement;

import java.time.LocalDate;
import java.util.UUID;

public interface IndemnityAgreementFactory {
    IndemnityAgreement create(UUID id, Customer customer, LocalDate sentOn);
}
