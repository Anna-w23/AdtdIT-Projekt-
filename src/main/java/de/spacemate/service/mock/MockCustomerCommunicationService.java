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
    private final Random random = new Random();

    private final Map<UUID, LocalDateTime> respondAt = new LinkedHashMap<>();
    private final Map<UUID, CustomerResponse> preparedResponses = new LinkedHashMap<>();

    public MockCustomerCommunicationService(CustomerRepository customerRepository,
                                            DocumentRepository documentRepository,
                                            CustomerAvailabilityRepository availabilityRepository,
                                            SimulatedClock clock,
                                            SimulationConfig simulationConfig) {
        this.customerRepository = customerRepository;
        this.documentRepository = documentRepository;
        this.availabilityRepository = availabilityRepository;
        this.clock = clock;
        this.simulationConfig = simulationConfig;
    }

    @Override
    public CustomerResponse sendQuestionnaire(UUID customerId) {
        Customer customer = getCustomer(customerId);
        LocalDateTime now = clock.now();

        Document outbound = new Document(UUID.randomUUID(), customerId,
                DocumentType.CORRESPONDENCE, DocumentCategory.QUESTIONNAIRE,
                DocumentDirection.OUTBOUND, now, "Questionnaire sent to " + customer.getFullName());
        documentRepository.save(outbound);

        Document inbound = new Document(UUID.randomUUID(), customerId,
                DocumentType.CORRESPONDENCE, DocumentCategory.QUESTIONNAIRE,
                DocumentDirection.INBOUND, now, "Questionnaire completed by " + customer.getFullName());
        documentRepository.save(inbound);

        generateCustomerAvailability(customerId);

        return new CustomerResponse(ResponseOutcome.ACCEPTED, "Questionnaire returned");
    }

    @Override
    public void sendAppointmentProposal(UUID customerId, UUID appointmentId) {
        Customer customer = getCustomer(customerId);
        LocalDateTime now = clock.now();

        Document outbound = new Document(UUID.randomUUID(), customerId,
                DocumentType.CORRESPONDENCE, DocumentCategory.APPOINTMENT_PROPOSAL,
                DocumentDirection.OUTBOUND, now, "Appointment proposal sent");
        outbound.putMetadata("appointmentId", appointmentId.toString());
        documentRepository.save(outbound);

        double refusalRate = customer.isIndemnityAgreementSigned() ? 0.50 : 0.30;
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
        LocalDateTime now = clock.now();
        CustomerResponse response = preparedResponses.remove(appointmentId);
        respondAt.remove(appointmentId);

        if (response == null) {
            throw new IllegalStateException("No pending response for appointment: " + appointmentId);
        }

        Document inbound = new Document(UUID.randomUUID(), customerId,
                DocumentType.CORRESPONDENCE, DocumentCategory.APPOINTMENT_RESPONSE,
                DocumentDirection.INBOUND, now, response.message());
        inbound.putMetadata("appointmentId", appointmentId.toString());
        inbound.putMetadata("outcome", response.outcome().name());
        documentRepository.save(inbound);

        return response;
    }

    @Override
    public void sendAppointmentReminder(UUID customerId, UUID appointmentId) {
        Customer customer = getCustomer(customerId);
        LocalDateTime now = clock.now();

        Document outbound = new Document(UUID.randomUUID(), customerId,
                DocumentType.CORRESPONDENCE, DocumentCategory.APPOINTMENT_PROPOSAL,
                DocumentDirection.OUTBOUND, now, "Reminder sent for appointment proposal");
        outbound.putMetadata("appointmentId", appointmentId.toString());
        outbound.putMetadata("type", "reminder");
        documentRepository.save(outbound);

        respondAt.put(appointmentId, now);
    }

    @Override
    public CustomerResponse sendIndemnityAgreement(UUID customerId) {
        Customer customer = getCustomer(customerId);
        LocalDateTime now = clock.now();

        Document outbound = new Document(UUID.randomUUID(), customerId,
                DocumentType.CORRESPONDENCE, DocumentCategory.INDEMNITY_AGREEMENT,
                DocumentDirection.OUTBOUND, now, "Indemnity agreement sent to " + customer.getFullName());
        documentRepository.save(outbound);

        return new CustomerResponse(ResponseOutcome.ACCEPTED, "Indemnity agreement sent");
    }

    @Override
    public CustomerResponse collectIndemnityResponse(UUID customerId) {
        Customer customer = getCustomer(customerId);
        LocalDateTime now = clock.now();

        boolean signed = random.nextDouble() < 0.80;
        ResponseOutcome outcome = signed ? ResponseOutcome.ACCEPTED : ResponseOutcome.REFUSED;
        String message = signed ? "Customer signed indemnity agreement" : "Customer refused indemnity agreement";

        Document inbound = new Document(UUID.randomUUID(), customerId,
                DocumentType.CORRESPONDENCE, DocumentCategory.INDEMNITY_RESPONSE,
                DocumentDirection.INBOUND, now, message);
        inbound.putMetadata("outcome", outcome.name());
        documentRepository.save(inbound);

        return new CustomerResponse(outcome, message);
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
