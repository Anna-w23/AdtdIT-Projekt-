package de.spacemate.orchestration;

import de.spacemate.model.*;
import de.spacemate.repository.AppointmentRepository;
import de.spacemate.repository.CustomerRepository;
import de.spacemate.repository.DocumentAttachmentRepository;
import de.spacemate.repository.DocumentRepository;
import de.spacemate.repository.MedicalReportRepository;
import de.spacemate.service.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class OnboardingOrchestrator {

    private final CustomerRepository customerRepository;
    private final MedicalReportRepository medicalReportRepository;
    private final AppointmentRepository appointmentRepository;
    private final DocumentRepository documentRepository;
    private final DocumentAttachmentRepository documentAttachmentRepository;
    private final RegistrationService registrationService;
    private final IndemnityService indemnityService;
    private final SchedulingService schedulingService;
    private final MedicalService medicalService;
    private final TrainingService trainingService;
    private final ResourceAssignmentService resourceAssignmentService;
    private final AppointmentCommunicationService communicationService;
    private final AppointmentReportService appointmentReportService;
    private final AppointmentSimulator simulationService;
    private final RefusalHandler refusalHandler;

    private final List<OnboardingEventListener> listeners = new ArrayList<>();

    public OnboardingOrchestrator(CustomerRepository customerRepository,
                                  MedicalReportRepository medicalReportRepository,
                                  AppointmentRepository appointmentRepository,
                                  DocumentRepository documentRepository,
                                  DocumentAttachmentRepository documentAttachmentRepository,
                                  RegistrationService registrationService,
                                  IndemnityService indemnityService,
                                  SchedulingService schedulingService,
                                  MedicalService medicalService,
                                  TrainingService trainingService,
                                  ResourceAssignmentService resourceAssignmentService,
                                  AppointmentCommunicationService communicationService,
                                  AppointmentReportService appointmentReportService,
                                  AppointmentSimulator simulationService,
                                  RefusalHandler refusalHandler) {
        this.customerRepository = customerRepository;
        this.medicalReportRepository = medicalReportRepository;
        this.appointmentRepository = appointmentRepository;
        this.documentRepository = documentRepository;
        this.documentAttachmentRepository = documentAttachmentRepository;
        this.registrationService = registrationService;
        this.indemnityService = indemnityService;
        this.schedulingService = schedulingService;
        this.medicalService = medicalService;
        this.trainingService = trainingService;
        this.resourceAssignmentService = resourceAssignmentService;
        this.communicationService = communicationService;
        this.appointmentReportService = appointmentReportService;
        this.simulationService = simulationService;
        this.refusalHandler = refusalHandler;
    }

    public void addEventListener(OnboardingEventListener listener) {
        listeners.add(listener);
    }

    private void fire(OnboardingEventType type, Customer customer, String message) {
        OnboardingEvent event = new OnboardingEvent(type, customer, message);
        for (OnboardingEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }

    // -------------------------------------------------------------------------
    // Registration & Questionnaire
    // -------------------------------------------------------------------------

    public Customer registerCustomer(String firstName, String lastName,
                                     String email, String language) {
        Customer customer = registrationService.registerCustomer(firstName, lastName, email, language);
        fire(OnboardingEventType.CUSTOMER_REGISTERED, customer, "Customer registered.");
        return customer;
    }

    public void saveCustomer(Customer customer) {
        customerRepository.save(customer);
    }

    public void sendQuestionnaire(UUID customerId) {
        registrationService.sendQuestionnaire(customerId);
        Customer customer = getCustomer(customerId);
        customer.setNeedsAttention(true, "Questionnaire completed – schedule initial medical");
        customerRepository.save(customer);
        fire(OnboardingEventType.QUESTIONNAIRE_COMPLETED, customer,
                "Questionnaire completed. AI reports generated. Ready to schedule first medical.");
    }

    // -------------------------------------------------------------------------
    // Appointment Scheduling
    // -------------------------------------------------------------------------

    public Appointment scheduleAppointmentByDrag(UUID customerId, UUID staffId,
                                                  LocalDateTime start, LocalDateTime end,
                                                  AppointmentType type) {
        Appointment appointment = schedulingService.scheduleByDrag(customerId, staffId, start, end, type);
        Customer customer = getCustomer(customerId);
        customer.setNeedsAttention(false, null);
        customerRepository.save(customer);
        fire(OnboardingEventType.APPOINTMENT_SUGGESTED, customer,
                type.displayName() + " proposed – send to customer.");
        return appointment;
    }

    public void sendProposalToCustomer(UUID customerId, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));

        schedulingService.markSent(appointmentId);
        communicationService.sendAppointmentProposal(customerId, appointmentId);

        Customer customer = getCustomer(customerId);
        customer.setNeedsAttention(false, null);
        customerRepository.save(customer);

        fire(OnboardingEventType.APPOINTMENT_SENT, customer,
                appointment.getType().displayName() + " proposal sent to customer.");
    }

    public boolean processCustomerResponse(UUID customerId, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));

        CustomerResponse response = communicationService.collectAppointmentResponse(customerId, appointmentId);
        Customer customer = getCustomer(customerId);

        // OCP: ResponseOutcome is domain-complete (3 values); a Strategy adds indirection without extensibility
        switch (response.outcome()) {
            case ACCEPTED -> {
                schedulingService.confirmAppointment(appointmentId);
                resourceAssignmentService.autoAssignResources(appointment);
                customer.setCurrentStage(appointment.getType().scheduledStage());
                customer.setNeedsAttention(false, null);
                customerRepository.save(customer);
                fire(OnboardingEventType.APPOINTMENT_CONFIRMED, customer,
                        appointment.getType().displayName() + " confirmed by customer.");
                return true;
            }
            case REFUSED_TIMESLOT -> {
                refuseTimeslot(customerId, appointmentId);
                return false;
            }
            case REFUSED_STEP -> {
                refuseAppointmentType(customerId, appointmentId);
                return false;
            }
            default -> { return false; }
        }
    }

    public void sendReminder(UUID customerId, UUID appointmentId) {
        communicationService.sendAppointmentReminder(customerId, appointmentId);
        Customer customer = getCustomer(customerId);
        fire(OnboardingEventType.APPOINTMENT_SENT, customer, "Reminder sent – awaiting response.");
    }

    public void collectPendingResponses() {
        List<Appointment> sentAppointments = appointmentRepository.findByStatus(AppointmentStatus.SENT);
        for (Appointment appointment : sentAppointments) {
            if (communicationService.hasPendingResponse(appointment.getId())) {
                processCustomerResponse(appointment.getCustomerId(), appointment.getId());
            }
        }
    }

    public void cancelAppointment(UUID customerId, UUID appointmentId) {
        resourceAssignmentService.removeAllAssignments(appointmentId);
        schedulingService.cancelAppointment(appointmentId);
        Customer customer = getCustomer(customerId);
        fire(OnboardingEventType.APPOINTMENT_CANCELLED, customer,
                "Appointment cancelled and slot released.");
    }

    public void cancelScheduledAppointment(UUID customerId, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
        String typeName = appointment.getType().displayName();
        resourceAssignmentService.removeAllAssignments(appointmentId);
        schedulingService.cancelAppointment(appointmentId);
        Customer customer = getCustomer(customerId);
        OnboardingStage rolled = rollbackStage(customer.getCurrentStage());
        customer.setCurrentStage(rolled);
        customer.setNeedsAttention(true, "Reschedule " + typeName + " required");
        customerRepository.save(customer);
        fire(OnboardingEventType.APPOINTMENT_CANCELLED, customer,
                typeName + " cancelled – reschedule required.");
    }

    public void discardSuggestion(UUID customerId, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
        String typeName = appointment.getType().displayName();
        resourceAssignmentService.removeAllAssignments(appointmentId);
        schedulingService.cancelAppointment(appointmentId);
        Customer customer = getCustomer(customerId);
        fire(OnboardingEventType.APPOINTMENT_CANCELLED, customer,
                typeName + " proposal discarded – select a different slot.");
    }

    // -------------------------------------------------------------------------
    // Refusal handling
    // -------------------------------------------------------------------------

    public void refuseTimeslot(UUID customerId, UUID appointmentId) {
        RefusalHandler.RefusalResult result = refusalHandler.refuseTimeslot(customerId, appointmentId);
        fire(OnboardingEventType.APPOINTMENT_TIMESLOT_REFUSED, result.customer(), result.message());
    }

    public void refuseAppointmentType(UUID customerId, UUID appointmentId) {
        RefusalHandler.RefusalResult result = refusalHandler.refuseAppointmentType(customerId, appointmentId);
        fire(OnboardingEventType.APPOINTMENT_TYPE_REFUSED, result.customer(), result.message());
    }

    // -------------------------------------------------------------------------
    // Indemnity
    // -------------------------------------------------------------------------

    public void offerIndemnityAgreement(UUID customerId) {
        UUID agreementId = indemnityService.sendIndemnityAgreement(customerId);
        CustomerResponse response = indemnityService.processIndemnityResponse(customerId, agreementId);
        Customer customer = getCustomer(customerId);

        if (response.outcome() == ResponseOutcome.ACCEPTED) {
            AppointmentType refusedType = getLastRefusedAppointmentType(customerId);
            if (refusedType != null) {
                skipToNextStage(customer, refusedType);
            }
            fire(OnboardingEventType.INDEMNITY_SIGNED, customer,
                    "Indemnity signed. Advancing to next step.");
        } else {
            fire(OnboardingEventType.CUSTOMER_REJECTED, customer,
                    "Customer refused indemnity agreement. Onboarding failed.");
        }
    }

    private void skipToNextStage(Customer customer, AppointmentType skippedType) {
        if (skippedType == AppointmentType.FINAL_MEDICAL) {
            customer.setCurrentStage(OnboardingStage.APPROVED);
        } else {
            customer.setCurrentStage(skippedType.completedStage());
        }
        customer.setIndemnityAgreementSigned(true);
        customer.setNeedsAttention(true,
                skippedType.displayName() + " skipped (indemnity signed)");
        customerRepository.save(customer);
    }

    // -------------------------------------------------------------------------
    // Simulate result (appointment outcomes)
    // -------------------------------------------------------------------------

    public void processAppointmentOutcome(UUID customerId, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));

        if (!resourceAssignmentService.hasRequiredResources(appointmentId, appointment)) {
            schedulingService.cancelAppointment(appointmentId);
            Customer c = getCustomer(customerId);
            OnboardingStage rolledBack = determineRollbackStage(customerId, appointment);
            c.setCurrentStage(rolledBack);
            c.setNeedsAttention(true,
                    appointment.getType().displayName() + " failed – required resources not assigned. Reschedule required.");
            customerRepository.save(c);
            fire(OnboardingEventType.SIMULATE_RESULT, c,
                    appointment.getType().displayName() + " failed: required resources not assigned.");
            return;
        }

        SimulationResult result = simulationService.simulate(customerId, appointment);
        Customer customer = getCustomer(customerId);

        if (result.passed()) {
            customer.setNeedsAttention(true, appointment.getType().displayName() + " completed – review and schedule next");
            customerRepository.save(customer);
            if (appointment.getType() == AppointmentType.FINAL_MEDICAL) {
                fire(OnboardingEventType.CUSTOMER_APPROVED, customer, result.message());
            } else if (appointment.getType() == AppointmentType.INITIAL_MEDICAL) {
                fire(OnboardingEventType.MEDICAL_RESULT_RECORDED, customer, result.message());
            } else {
                fire(OnboardingEventType.APPOINTMENT_COMPLETED, customer, result.message());
            }
        } else {
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointmentRepository.save(appointment);
            customer.setCurrentStage(OnboardingStage.APPOINTMENT_REFUSED);
            customer.setNeedsAttention(true,
                    appointment.getType().displayName() + " failed – offer indemnity agreement.");
            customerRepository.save(customer);
            fire(OnboardingEventType.SIMULATE_RESULT, customer, result.message());
        }
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public List<Appointment> getAppointmentsForCustomer(UUID customerId) {
        return schedulingService.getAppointmentsForCustomer(customerId);
    }

    public List<Appointment> findByDateAndStatus(LocalDate date, AppointmentStatus status) {
        return appointmentRepository.findByDateAndStatus(date, status);
    }

    public Set<LocalDateTime> getCustomerAvailability(UUID customerId) {
        return schedulingService.getCustomerAvailability(customerId);
    }

    public List<StaffAvailability> getStaffAvailability(UUID staffId) {
        return schedulingService.getStaffAvailability(staffId);
    }

    public List<TimeSlot> getBookedSlotsByStaffAndDate(UUID staffId, LocalDate date) {
        return schedulingService.getBookedSlotsByStaffAndDate(staffId, date);
    }

    // OCP: workflow graph logic — transitions encode domain rules that a State pattern (17 classes) would not simplify
    public List<AppointmentType> getRequiredNextAppointments(UUID customerId) {
        List<AppointmentType> required = new ArrayList<>();
        Customer customer = getCustomer(customerId);
        OnboardingStage stage = customer.getCurrentStage();

        if (stage == OnboardingStage.QUESTIONNAIRE_COMPLETED) {
            required.add(AppointmentType.INITIAL_MEDICAL);
        } else if (stage == OnboardingStage.FIRST_MEDICAL_COMPLETED) {
            List<AppointmentType> remaining = getRemainingSpecialists(customerId);
            if (remaining.isEmpty()) {
                required.add(AppointmentType.SPACE_TRAINING);
            } else {
                required.addAll(remaining);
            }
        } else if (stage == OnboardingStage.SPECIALIST_COMPLETED) {
            List<AppointmentType> remaining = getRemainingSpecialists(customerId);
            if (remaining.isEmpty()) {
                required.add(AppointmentType.SPACE_TRAINING);
            } else {
                required.addAll(remaining);
            }
        } else if (stage == OnboardingStage.SPACE_TRAINING_COMPLETED) {
            required.add(AppointmentType.FINAL_MEDICAL);
        } else if (stage == OnboardingStage.INDEMNITY_SIGNED) {
            required.addAll(getNextAfterCurrentStage(customerId));
        }

        return required;
    }

    public List<AppointmentType> getRemainingSpecialists(UUID customerId) {
        List<AppointmentType> allRequired = medicalReportRepository.findLatestByCustomerId(customerId)
                .map(MedicalReport::getRequiredSpecialists)
                .orElse(List.of());

        Set<AppointmentType> completed = schedulingService.getAppointmentsForCustomer(customerId).stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .filter(a -> a.getType().isSpecialist())
                .map(Appointment::getType)
                .collect(Collectors.toSet());

        return allRequired.stream()
                .filter(t -> !completed.contains(t))
                .toList();
    }

    // OCP: part of the workflow graph — same reasoning as getRequiredNextAppointments
    private List<AppointmentType> getNextAfterCurrentStage(UUID customerId) {
        Customer customer = getCustomer(customerId);
        OnboardingStage stage = customer.getCurrentStage();
        if (stage == OnboardingStage.INDEMNITY_SIGNED) {
            AppointmentType skipped = getLastRefusedAppointmentType(customerId);
            if (skipped == null) return List.of();
            OnboardingStage landed = skipped.completedStage();
            if (landed == OnboardingStage.FIRST_MEDICAL_COMPLETED) {
                List<AppointmentType> remaining = getRemainingSpecialists(customerId);
                return remaining.isEmpty() ? List.of(AppointmentType.SPACE_TRAINING) : remaining;
            } else if (landed == OnboardingStage.SPECIALIST_COMPLETED) {
                List<AppointmentType> remaining = getRemainingSpecialists(customerId);
                return remaining.isEmpty() ? List.of(AppointmentType.SPACE_TRAINING) : remaining;
            } else if (landed == OnboardingStage.SPACE_TRAINING_COMPLETED) {
                return List.of(AppointmentType.FINAL_MEDICAL);
            } else if (landed == OnboardingStage.FINAL_MEDICAL_COMPLETED) {
                return List.of();
            }
        }
        return List.of();
    }

    // OCP: progress count is tightly coupled to the fixed workflow; extracting it would obscure the logic
    public int remainingSteps(UUID customerId) {
        Customer customer = getCustomer(customerId);
        OnboardingStage stage = customer.getCurrentStage();

        boolean needsSpecialist = medicalReportRepository.findLatestByCustomerId(customerId)
                .map(MedicalReport::requiresSpecialists)
                .orElse(true);

        return switch (stage) {
            case REGISTERED, QUESTIONNAIRE_SENT, QUESTIONNAIRE_COMPLETED, FIRST_MEDICAL_SCHEDULED
                    -> needsSpecialist ? 4 : 3;
            case FIRST_MEDICAL_COMPLETED, APPOINTMENT_REFUSED -> needsSpecialist ? 3 : 2;
            case SPECIALIST_SCHEDULED -> 3;
            case SPECIALIST_COMPLETED, SPACE_TRAINING_SCHEDULED -> 2;
            case SPACE_TRAINING_COMPLETED, FINAL_MEDICAL_SCHEDULED -> 1;
            case FINAL_MEDICAL_COMPLETED, INDEMNITY_PENDING, INDEMNITY_SIGNED,
                    APPROVED, REJECTED, FAILED -> 0;
        };
    }

    public Appointment getAppointmentById(UUID appointmentId) {
        return appointmentRepository.findById(appointmentId).orElse(null);
    }

    public AppointmentType getLastRefusedAppointmentType(UUID customerId) {
        return schedulingService.getAppointmentsForCustomer(customerId).stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CANCELLED)
                .reduce((first, second) -> second)
                .map(Appointment::getType)
                .orElse(null);
    }

    // -------------------------------------------------------------------------
    // Resource management
    // -------------------------------------------------------------------------

    public void assignResource(UUID appointmentId, UUID resourceId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
        LocalDateTime start = appointment.getTimeSlot().getStart();
        LocalDateTime end = appointment.getTimeSlot().getEnd();
        resourceAssignmentService.assignResource(appointmentId, resourceId, start, end);
    }

    public void removeResourceAssignment(UUID appointmentId, UUID resourceId) {
        resourceAssignmentService.removeAssignment(appointmentId, resourceId);
    }

    public List<ResourceAssignment> getResourceAssignments(UUID appointmentId) {
        return resourceAssignmentService.getAssignments(appointmentId);
    }

    public List<Resource> getAvailableResourcesForAppointment(UUID appointmentId,
                                                               ResourceCategory category, String tag) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
        LocalDateTime start = appointment.getTimeSlot().getStart();
        LocalDateTime end = appointment.getTimeSlot().getEnd();
        return resourceAssignmentService.getAvailableResources(category, tag, start, end, appointmentId);
    }

    public boolean hasRequiredResources(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
        return resourceAssignmentService.hasRequiredResources(appointmentId, appointment);
    }

    public List<ResourceAvailability> getResourceAvailability(UUID resourceId) {
        return resourceAssignmentService.getResourceAvailability(resourceId);
    }

    public List<ResourceAssignment> getBookedResourceSlots(UUID resourceId, LocalDate date) {
        return resourceAssignmentService.getBookedResourceSlots(resourceId, date);
    }

    public List<ResourceRequirement> getResourceRequirements(AppointmentType type) {
        return resourceAssignmentService.getResourceRequirements(type);
    }

    public List<ResourceRequirement> getResourceRequirements(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
        return resourceAssignmentService.getResourceRequirements(appointment);
    }

    public List<Document> getDocumentsForCustomer(UUID customerId) {
        return documentRepository.findByCustomerId(customerId);
    }

    // -------------------------------------------------------------------------
    // Document attachments
    // -------------------------------------------------------------------------

    public void attachDocumentToAppointment(UUID appointmentId, UUID documentId) {
        DocumentAttachment attachment = new DocumentAttachment(
                UUID.randomUUID(), appointmentId, documentId);
        documentAttachmentRepository.save(attachment);
    }

    public void detachDocumentFromAppointment(UUID appointmentId, UUID documentId) {
        documentAttachmentRepository.deleteByAppointmentAndDocument(appointmentId, documentId);
    }

    public List<DocumentAttachment> getDocumentAttachments(UUID appointmentId) {
        return documentAttachmentRepository.findByAppointmentId(appointmentId);
    }

    public List<Document> getAttachableDocuments(UUID customerId, UUID appointmentId) {
        List<Document> reports = documentRepository.findByCustomerId(customerId).stream()
                .filter(d -> d.getType() == DocumentType.REPORT)
                .toList();

        Set<UUID> alreadyAttached = documentAttachmentRepository.findByAppointmentId(appointmentId).stream()
                .map(DocumentAttachment::getDocumentId)
                .collect(Collectors.toSet());

        return reports.stream()
                .filter(d -> !alreadyAttached.contains(d.getId()))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private OnboardingStage rollbackStage(OnboardingStage scheduled) {
        return RefusalHandler.rollbackStage(scheduled);
    }

    private OnboardingStage determineRollbackStage(UUID customerId, Appointment appointment) {
        OnboardingStage generic = rollbackStage(appointment.getType().scheduledStage());
        if (appointment.getType().isSpecialist() && hasCompletedSpecialists(customerId)) {
            return OnboardingStage.SPECIALIST_COMPLETED;
        }
        return generic;
    }

    private boolean hasCompletedSpecialists(UUID customerId) {
        return schedulingService.getAppointmentsForCustomer(customerId).stream()
                .anyMatch(a -> a.getType().isSpecialist() && a.getStatus() == AppointmentStatus.COMPLETED);
    }

    private Customer getCustomer(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
    }

    public boolean isScheduledStage(OnboardingStage stage) {
        return stage == OnboardingStage.FIRST_MEDICAL_SCHEDULED
                || stage == OnboardingStage.SPECIALIST_SCHEDULED
                || stage == OnboardingStage.SPACE_TRAINING_SCHEDULED
                || stage == OnboardingStage.FINAL_MEDICAL_SCHEDULED;
    }
}
