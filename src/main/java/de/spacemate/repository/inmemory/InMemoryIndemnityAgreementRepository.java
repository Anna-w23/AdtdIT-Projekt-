package de.spacemate.repository.inmemory;

import de.spacemate.model.IndemnityAgreement;
import de.spacemate.repository.IndemnityAgreementRepository;

import java.util.*;

public class InMemoryIndemnityAgreementRepository implements IndemnityAgreementRepository {

    private final Map<UUID, IndemnityAgreement> byCustomer = new LinkedHashMap<>();
    private final Map<UUID, IndemnityAgreement> byId = new LinkedHashMap<>();

    @Override
    public void save(IndemnityAgreement agreement) {
        byCustomer.put(agreement.getCustomerId(), agreement);
        byId.put(agreement.getId(), agreement);
    }

    @Override
    public Optional<IndemnityAgreement> findById(UUID agreementId) {
        return Optional.ofNullable(byId.get(agreementId));
    }

    @Override
    public Optional<IndemnityAgreement> findByCustomerId(UUID customerId) {
        return Optional.ofNullable(byCustomer.get(customerId));
    }
}
