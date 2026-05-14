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
    private final CustomerCommunicationService communicationService;
    private final AppointmentReportService appointmentReportService;

    private final List<OnboardingEventListener> listeners = new ArrayList<>();
    private final Random random = new Random();

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
                                  CustomerCommunicationService communicationService,
                                  AppointmentReportService appointmentReportService) {
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
    }

    public void addEventListener(OnboardingEventListener listener) {
        listeners.add(listener);
    }

    public void removeEventListener(OnboardingEventListener listener) {
        listeners.remove(listener);
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
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));

        LocalDateTime slotStart = appointment.getTimeSlot().getStart();
        LocalDateTime slotEnd = appointment.getTimeSlot().getEnd();

        schedulingService.cancelAppointment(appointmentId);
        schedulingService.removeCustomerTimeslot(customerId, slotStart, slotEnd);

        Customer customer = getCustomer(customerId);
        OnboardingStage rolledBack = rollbackStage(appointment.getType().scheduledStage());
        customer.setCurrentStage(rolledBack);
        customer.setNeedsAttention(true,
                "Customer disagrees with timeslot for " + appointment.getType().displayName() + " – reschedule required");
        customerRepository.save(customer);

        fire(OnboardingEventType.APPOINTMENT_TIMESLOT_REFUSED, customer,
                "Customer disagreed with timeslot. Reschedule " + appointment.getType().displayName() + ".");
    }

    public void refuseAppointmentType(UUID customerId, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));

        schedulingService.cancelAppointment(appointmentId);
        Customer customer = getCustomer(customerId);

        customer.setCurrentStage(OnboardingStage.APPOINTMENT_REFUSED);
        customer.setNeedsAttention(true,
                "Customer refused " + appointment.getType().displayName() + " – offer indemnity agreement");
        customerRepository.save(customer);
        fire(OnboardingEventType.APPOINTMENT_TYPE_REFUSED, customer,
                "Customer refused " + appointment.getType().displayName() + ". Offer indemnity agreement.");
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

    public void simulateResult(UUID customerId, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));

        if (!resourceAssignmentService.hasRequiredResources(appointmentId, appointment)) {
            schedulingService.cancelAppointment(appointmentId);
            Customer c = getCustomer(customerId);
            c.setCurrentStage(rollbackStage(appointment.getType().scheduledStage()));
            c.setNeedsAttention(true,
                    appointment.getType().displayName() + " failed – required resources not assigned. Reschedule required.");
            customerRepository.save(c);
            fire(OnboardingEventType.SIMULATE_RESULT, c,
                    appointment.getType().displayName() + " failed: required resources not assigned.");
            return;
        }

        switch (appointment.getType()) {
            case INITIAL_MEDICAL -> simulateInitialMedical(customerId, appointmentId);
            case FINAL_MEDICAL -> simulateFinalMedical(customerId, appointmentId);
            case EYE_SPECIALIST, CARDIOLOGIST, NEUROLOGIST, ORTHOPEDIST, PSYCHOLOGIST_CONSULTATION ->
                    simulateSpecialist(customerId, appointmentId, appointment.getType());
            case SPACE_TRAINING -> simulateSpaceTraining(customerId, appointmentId);
        }
    }

    private void simulateInitialMedical(UUID customerId, UUID appointmentId) {
        Map<SpecialistArea, Boolean> results = new EnumMap<>(SpecialistArea.class);
        for (SpecialistArea area : SpecialistArea.values()) {
            results.put(area, random.nextInt(5) != 0);
        }

        boolean needsExtended = random.nextInt(4) == 0;

        // Check if AI trainer report flagged extended training
        List<Document> trainerReports = documentRepository.findByCustomerIdAndCategory(
                customerId, DocumentCategory.AI_TRAINER_REPORT);
        if (!trainerReports.isEmpty()) {
            Document latest = trainerReports.get(trainerReports.size() - 1);
            if ("true".equals(latest.getMetadataValue("needsExtendedTraining"))) {
                needsExtended = true;
            }
        }

        MedicalReport report = medicalService.recordInitialMedicalResult(
                customerId, appointmentId, results, needsExtended, "Auto-generated result.");
        appointmentReportService.createInitialMedicalReport(customerId, report);

        Customer customer = getCustomer(customerId);
        List<SpecialistArea> failed = report.getFailedAreas();
        StringBuilder msg = new StringBuilder("Initial medical completed.");
        if (!failed.isEmpty()) {
            msg.append(" Specialists needed: ");
            msg.append(failed.stream().map(SpecialistArea::displayName)
                    .reduce((a, b) -> a + ", " + b).orElse(""));
            msg.append(".");
        }
        if (needsExtended) {
            msg.append(" Extended space training required.");
        }
        if (failed.isEmpty() && !needsExtended) {
            msg.append(" All clear – proceed to space training.");
        }

        customer.setNeedsAttention(true, "Initial Medical completed – review report and schedule next");
        customerRepository.save(customer);
        fire(OnboardingEventType.MEDICAL_RESULT_RECORDED, customer, msg.toString());
    }

    private void simulateFinalMedical(UUID customerId, UUID appointmentId) {
        boolean eligible = random.nextInt(5) != 0;
        MedicalReport report = medicalService.recordFinalMedicalResult(
                customerId, appointmentId, eligible, "Auto-generated result.");
        appointmentReportService.createFinalMedicalReport(customerId, report);
        Customer customer = getCustomer(customerId);

        if (eligible) {
            fire(OnboardingEventType.CUSTOMER_APPROVED, customer,
                    "Final medical passed. Customer approved for flight.");
        } else {
            appointmentRepository.findById(appointmentId).ifPresent(a -> {
                a.setStatus(AppointmentStatus.CANCELLED);
                appointmentRepository.save(a);
            });
            customer.setCurrentStage(OnboardingStage.APPOINTMENT_REFUSED);
            customer.setNeedsAttention(true,
                    "Final Medical failed – customer not flight eligible. Offer indemnity agreement.");
            customerRepository.save(customer);
            fire(OnboardingEventType.SIMULATE_RESULT, customer,
                    "Final medical failed. Offer indemnity agreement.");
        }
    }

    private void simulateSpecialist(UUID customerId, UUID appointmentId, AppointmentType type) {
        boolean passed = random.nextInt(10) < 7;
        appointmentReportService.createSpecialistReport(customerId, appointmentId, type, passed);
        Customer customer = getCustomer(customerId);

        if (passed) {
            trainingService.completeSpecialistConsultation(customerId, appointmentId);
            customer = getCustomer(customerId);
            customer.setNeedsAttention(true, type.displayName() + " passed – check remaining specialists");
            customerRepository.save(customer);
            fire(OnboardingEventType.APPOINTMENT_COMPLETED, customer,
                    type.displayName() + " cleared. Proceed to next step.");
        } else {
            appointmentRepository.findById(appointmentId).ifPresent(a -> {
                a.setStatus(AppointmentStatus.CANCELLED);
                appointmentRepository.save(a);
            });
            customer.setCurrentStage(OnboardingStage.APPOINTMENT_REFUSED);
            customer.setNeedsAttention(true,
                    type.displayName() + " failed – customer not cleared. Offer indemnity agreement.");
            customerRepository.save(customer);
            fire(OnboardingEventType.SIMULATE_RESULT, customer,
                    type.displayName() + " failed. Offer indemnity agreement.");
        }
    }

    private void simulateSpaceTraining(UUID customerId, UUID appointmentId) {
        boolean passed = random.nextInt(5) < 4;
        appointmentReportService.createTrainingReport(customerId, appointmentId, passed);
        Customer customer = getCustomer(customerId);

        if (passed) {
            trainingService.completeSpaceTraining(customerId, appointmentId);
            customer = getCustomer(customerId);
            customer.setNeedsAttention(true, "Space Training passed – schedule Final Medical");
            customerRepository.save(customer);
            fire(OnboardingEventType.APPOINTMENT_COMPLETED, customer,
                    "Space training completed. Ready for final medical.");
        } else {
            appointmentRepository.findById(appointmentId).ifPresent(a -> {
                a.setStatus(AppointmentStatus.CANCELLED);
                appointmentRepository.save(a);
            });
            customer.setCurrentStage(OnboardingStage.APPOINTMENT_REFUSED);
            customer.setNeedsAttention(true,
                    "Space Training failed – customer did not pass. Offer indemnity agreement.");
            customerRepository.save(customer);
            fire(OnboardingEventType.SIMULATE_RESULT, customer,
                    "Space training failed. Offer indemnity agreement.");
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

    public List<AppointmentType> getRequiredNextAppointments(UUID customerId) {
        List<AppointmentType> required = new ArrayList<>();
        Customer customer = getCustomer(customerId);
        OnboardingStage stage = customer.getCurrentStage();

        if (stage == OnboardingStage.QUESTIONNAIRE_COMPLETED) {
            required.add(AppointmentType.INITIAL_MEDICAL);
        } else if (stage == OnboardingStage.FIRST_MEDICAL_COMPLETED) {
            Optional<MedicalReport> reportOpt = medicalReportRepository.findLatestByCustomerId(customerId);
            if (reportOpt.isPresent()) {
                MedicalReport report = reportOpt.get();
                required.addAll(report.getRequiredSpecialists());
                if (!report.requiresSpecialists()) {
                    required.add(AppointmentType.SPACE_TRAINING);
                }
            } else {
                required.add(AppointmentType.SPACE_TRAINING);
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

        Set<AppointmentType> resolved = schedulingService.getAppointmentsForCustomer(customerId).stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED
                        || a.getStatus() == AppointmentStatus.CANCELLED)
                .filter(a -> a.getType().isSpecialist())
                .map(Appointment::getType)
                .collect(Collectors.toSet());

        return allRequired.stream()
                .filter(t -> !resolved.contains(t))
                .toList();
    }

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

    public boolean needsExtendedTraining(UUID customerId) {
        return medicalReportRepository.findLatestByCustomerId(customerId)
                .map(MedicalReport::isNeedsExtendedTraining)
                .orElse(false);
    }

    public int remainingSteps(UUID customerId) {
        Customer customer = getCustomer(customerId);
        OnboardingStage stage = customer.getCurrentStage();

        boolean needsSpecialist = medicalReportRepository.findLatestByCustomerId(customerId)
                .map(MedicalReport::requiresSpecialists)
                .orElse(true);

        return switch (stage) {
            case REGISTERED, QUESTIONNAIRE_SENT -> needsSpecialist ? 4 : 3;
            case QUESTIONNAIRE_COMPLETED -> needsSpecialist ? 4 : 3;
            case FIRST_MEDICAL_SCHEDULED -> needsSpecialist ? 4 : 3;
            case FIRST_MEDICAL_COMPLETED -> needsSpecialist ? 3 : 2;
            case SPECIALIST_SCHEDULED -> 3;
            case SPECIALIST_COMPLETED -> 2;
            case SPACE_TRAINING_SCHEDULED -> 2;
            case SPACE_TRAINING_COMPLETED -> 1;
            case FINAL_MEDICAL_SCHEDULED -> 1;
            case FINAL_MEDICAL_COMPLETED, INDEMNITY_PENDING, INDEMNITY_SIGNED -> 0;
            case APPROVED, REJECTED, FAILED -> 0;
            case APPOINTMENT_REFUSED -> needsSpecialist ? 3 : 2;
        };
    }

    public Appointment getAppointmentById(UUID appointmentId) {
        return appointmentRepository.findById(appointmentId).orElse(null);
    }

    public AppointmentType getLastRefusedAppointmentType(UUID customerId) {
        return schedulingService.getAppointmentsForCustomer(customerId).stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CANCELLED)
                .filter(a -> !a.getId().toString().startsWith("priv-"))
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
        return switch (scheduled) {
            case FIRST_MEDICAL_SCHEDULED  -> OnboardingStage.QUESTIONNAIRE_COMPLETED;
            case SPECIALIST_SCHEDULED     -> OnboardingStage.FIRST_MEDICAL_COMPLETED;
            case SPACE_TRAINING_SCHEDULED -> OnboardingStage.SPECIALIST_COMPLETED;
            case FINAL_MEDICAL_SCHEDULED  -> OnboardingStage.SPACE_TRAINING_COMPLETED;
            default                       -> scheduled;
        };
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
