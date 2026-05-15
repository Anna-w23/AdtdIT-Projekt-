package de.spacemate.service;

import de.spacemate.model.*;
import de.spacemate.repository.CustomerRepository;
import de.spacemate.repository.IndemnityAgreementRepository;
import de.spacemate.factory.IndemnityAgreementFactory;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class IndemnityService {

    private final CustomerRepository customerRepository;
    private final IndemnityAgreementRepository agreementRepository;
    private final IndemnityAgreementFactory agreementFactory;
    private final IndemnityCommunicationService communicationService;

    public IndemnityService(CustomerRepository customerRepository,
                            IndemnityAgreementRepository agreementRepository,
                            IndemnityAgreementFactory agreementFactory,
                            IndemnityCommunicationService communicationService) {
        this.customerRepository = customerRepository;
        this.agreementRepository = agreementRepository;
        this.agreementFactory = agreementFactory;
        this.communicationService = communicationService;
    }

    public UUID sendIndemnityAgreement(UUID customerId) {
        Customer customer = getCustomer(customerId);
        IndemnityAgreement agreement = agreementFactory.create(
                UUID.randomUUID(), customer, LocalDate.now());
        agreementRepository.save(agreement);

        customer.setCurrentStage(OnboardingStage.INDEMNITY_PENDING);
        customerRepository.save(customer);

        communicationService.sendIndemnityAgreement(customerId);
        return agreement.getId();
    }

    public CustomerResponse processIndemnityResponse(UUID customerId, UUID agreementId) {
        Customer customer = getCustomer(customerId);
        IndemnityAgreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new IllegalArgumentException("Agreement not found: " + agreementId));

        CustomerResponse response = communicationService.collectIndemnityResponse(customerId);

        if (response.outcome() == ResponseOutcome.ACCEPTED) {
            agreement.markSigned(LocalDate.now());
            agreementRepository.save(agreement);
            customer.setIndemnityAgreementSigned(true);
            customer.setCurrentStage(OnboardingStage.INDEMNITY_SIGNED);
            customerRepository.save(customer);
        } else {
            customer.setCurrentStage(OnboardingStage.FAILED);
            customer.setNeedsAttention(false, null);
            customerRepository.save(customer);
        }

        return response;
    }

    private Customer getCustomer(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
    }
}
