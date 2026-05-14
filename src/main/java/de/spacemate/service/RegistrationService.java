package de.spacemate.service;

import de.spacemate.factory.CustomerFactory;
import de.spacemate.model.*;
import de.spacemate.repository.CustomerRepository;
import de.spacemate.repository.DocumentRepository;

import java.util.List;
import java.util.UUID;

public class RegistrationService {

    private final CustomerRepository customerRepository;
    private final DocumentRepository documentRepository;
    private final CustomerCommunicationService communicationService;
    private final DocumentAnalysisService analysisService;
    private final CustomerFactory customerFactory;

    public RegistrationService(CustomerRepository customerRepository,
                               DocumentRepository documentRepository,
                               CustomerCommunicationService communicationService,
                               DocumentAnalysisService analysisService,
                               CustomerFactory customerFactory) {
        this.customerRepository = customerRepository;
        this.documentRepository = documentRepository;
        this.communicationService = communicationService;
        this.analysisService = analysisService;
        this.customerFactory = customerFactory;
    }

    public Customer registerCustomer(String firstName, String lastName, String email, String language) {
        Customer customer = customerFactory.create(UUID.randomUUID(), firstName, lastName, email);
        customer.setPreferredLanguage(language);
        customer.setCurrentStage(OnboardingStage.REGISTERED);
        customerRepository.save(customer);
        return customer;
    }

    public void sendQuestionnaire(UUID customerId) {
        Customer customer = getCustomer(customerId);
        customer.setCurrentStage(OnboardingStage.QUESTIONNAIRE_SENT);
        customerRepository.save(customer);

        CustomerResponse response = communicationService.sendQuestionnaire(customerId);

        if (response.outcome() == ResponseOutcome.ACCEPTED) {
            List<Document> questionnaires = documentRepository.findByCustomerIdAndCategory(
                    customerId, DocumentCategory.QUESTIONNAIRE);
            Document inboundQuestionnaire = questionnaires.stream()
                    .filter(d -> d.getDirection() == DocumentDirection.INBOUND)
                    .reduce((first, second) -> second)
                    .orElse(null);

            if (inboundQuestionnaire != null) {
                analysisService.analyseQuestionnaire(customerId, inboundQuestionnaire);
            }

            customer.setCurrentStage(OnboardingStage.QUESTIONNAIRE_COMPLETED);
            customerRepository.save(customer);
        }
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    private Customer getCustomer(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
    }
}
