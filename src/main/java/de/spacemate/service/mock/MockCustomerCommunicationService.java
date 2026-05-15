package de.spacemate.service.mock;

import de.spacemate.app.SimulatedClock;
import de.spacemate.app.SimulationConfig;
import de.spacemate.model.*;
import de.spacemate.repository.CustomerAvailabilityRepository;
import de.spacemate.repository.CustomerRepository;
import de.spacemate.repository.DocumentRepository;
import de.spacemate.service.CustomerCommunicationService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class MockCustomerCommunicationService implements CustomerCommunicationService {

    private final CustomerRepository customerRepository;
    private final DocumentRepository documentRepository;
    private final CustomerAvailabilityRepository availabilityRepository;
    private final SimulatedClock clock;
    private final SimulationConfig simulationConfig;
    private final Random random;

    private final Map<UUID, LocalDateTime> respondAt = new LinkedHashMap<>();
    private final Map<UUID, CustomerResponse> preparedResponses = new LinkedHashMap<>();

    public MockCustomerCommunicationService(CustomerRepository customerRepository,
                                            DocumentRepository documentRepository,
                                            CustomerAvailabilityRepository availabilityRepository,
                                            SimulatedClock clock,
                                            SimulationConfig simulationConfig,
                                            Random random) {
        this.customerRepository = customerRepository;
        this.documentRepository = documentRepository;
        this.availabilityRepository = availabilityRepository;
        this.clock = clock;
        this.simulationConfig = simulationConfig;
        this.random = random;
    }

    @Override
    public CustomerResponse sendQuestionnaire(UUID customerId) {
        Customer customer = getCustomer(customerId);

        createCorrespondence(customerId, DocumentCategory.QUESTIONNAIRE,
                DocumentDirection.OUTBOUND, "Questionnaire sent to " + customer.getFullName());
        createCorrespondence(customerId, DocumentCategory.QUESTIONNAIRE,
                DocumentDirection.INBOUND, "Questionnaire completed by " + customer.getFullName());

        generateCustomerAvailability(customerId);

        return new CustomerResponse(ResponseOutcome.ACCEPTED, "Questionnaire returned");
    }

    @Override
    public void sendAppointmentProposal(UUID customerId, UUID appointmentId) {
        Customer customer = getCustomer(customerId);
        LocalDateTime now = clock.now();

        Document outbound = createCorrespondence(customerId, DocumentCategory.APPOINTMENT_PROPOSAL,
                DocumentDirection.OUTBOUND, "Appointment proposal sent");
        outbound.putMetadata("appointmentId", appointmentId.toString());

        double refusalRate = customer.isIndemnityAgreementSigned() ? 0.15 : 0.08;
        double roll = random.nextDouble();

        ResponseOutcome outcome;
        String message;
        if (roll < (1.0 - refusalRate)) {
            outcome = ResponseOutcome.ACCEPTED;
            message = "Customer accepted the appointment";
        } else if (roll < (1.0 - refusalRate / 3.0)) {
            outcome = ResponseOutcome.REFUSED_TIMESLOT;
            message = "Customer refused the proposed timeslot";
        } else {
            outcome = ResponseOutcome.REFUSED_STEP;
            message = "Customer refused the appointment type entirely";
        }

        preparedResponses.put(appointmentId, new CustomerResponse(outcome, message));

        if (simulationConfig.isInstantResponse()) {
            respondAt.put(appointmentId, now);
        } else {
            int delayHours = 3 + random.nextInt(10);
            respondAt.put(appointmentId, now.plusHours(delayHours));
        }
    }

    @Override
    public boolean hasPendingResponse(UUID appointmentId) {
        LocalDateTime responseTime = respondAt.get(appointmentId);
        if (responseTime == null) return false;
        return !clock.now().isBefore(responseTime);
    }

    @Override
    public CustomerResponse collectAppointmentResponse(UUID customerId, UUID appointmentId) {
        CustomerResponse response = preparedResponses.remove(appointmentId);
        respondAt.remove(appointmentId);

        if (response == null) {
            throw new IllegalStateException("No pending response for appointment: " + appointmentId);
        }

        Document inbound = createCorrespondence(customerId, DocumentCategory.APPOINTMENT_RESPONSE,
                DocumentDirection.INBOUND, response.message());
        inbound.putMetadata("appointmentId", appointmentId.toString());
        inbound.putMetadata("outcome", response.outcome().name());

        return response;
    }

    @Override
    public void sendAppointmentReminder(UUID customerId, UUID appointmentId) {
        Customer customer = getCustomer(customerId);

        Document outbound = createCorrespondence(customerId, DocumentCategory.APPOINTMENT_PROPOSAL,
                DocumentDirection.OUTBOUND, "Reminder sent for appointment proposal");
        outbound.putMetadata("appointmentId", appointmentId.toString());
        outbound.putMetadata("type", "reminder");

        respondAt.put(appointmentId, clock.now());
    }

    @Override
    public CustomerResponse sendIndemnityAgreement(UUID customerId) {
        Customer customer = getCustomer(customerId);

        createCorrespondence(customerId, DocumentCategory.INDEMNITY_AGREEMENT,
                DocumentDirection.OUTBOUND, "Indemnity agreement sent to " + customer.getFullName());

        return new CustomerResponse(ResponseOutcome.ACCEPTED, "Indemnity agreement sent");
    }

    @Override
    public CustomerResponse collectIndemnityResponse(UUID customerId) {
        Customer customer = getCustomer(customerId);

        boolean signed = random.nextDouble() < 0.95;
        ResponseOutcome outcome = signed ? ResponseOutcome.ACCEPTED : ResponseOutcome.REFUSED;
        String message = signed ? "Customer signed indemnity agreement" : "Customer refused indemnity agreement";

        Document inbound = createCorrespondence(customerId, DocumentCategory.INDEMNITY_RESPONSE,
                DocumentDirection.INBOUND, message);
        inbound.putMetadata("outcome", outcome.name());

        return new CustomerResponse(outcome, message);
    }

    private Document createCorrespondence(UUID customerId, DocumentCategory category,
                                           DocumentDirection direction, String content) {
        Document doc = new Document(UUID.randomUUID(), customerId,
                DocumentType.CORRESPONDENCE, category, direction, clock.now(), content);
        documentRepository.save(doc);
        return doc;
    }

    private Customer getCustomer(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
    }

    private void generateCustomerAvailability(UUID customerId) {
        int[] workHours = {8, 9, 10, 11, 13, 14, 15, 16, 17};
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        Set<LocalDateTime> slots = new LinkedHashSet<>();

        for (int week = 0; week < 4; week++) {
            for (int day = 0; day < 5; day++) {
                LocalDate date = monday.plusWeeks(week).plusDays(day);
                if (random.nextInt(10) == 0) continue;

                int[] hours = workHours.clone();
                for (int i = hours.length - 1; i > 0; i--) {
                    int j = random.nextInt(i + 1);
                    int tmp = hours[i]; hours[i] = hours[j]; hours[j] = tmp;
                }
                int slotsPerDay = 3 + random.nextInt(4);
                for (int k = 0; k < slotsPerDay && k < hours.length; k++) {
                    slots.add(date.atTime(hours[k], 0));
                    slots.add(date.atTime(hours[k], 30));
                }
            }
        }
        availabilityRepository.setAvailability(customerId, slots);
    }
}
