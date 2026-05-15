package de.spacemate.service;

import de.spacemate.model.*;
import de.spacemate.repository.DocumentRepository;

import java.util.*;

public class AppointmentSimulationService implements AppointmentSimulator {

    private final DocumentRepository documentRepository;
    private final MedicalService medicalService;
    private final TrainingService trainingService;
    private final AppointmentReportService appointmentReportService;
    private final Random random;

    public AppointmentSimulationService(DocumentRepository documentRepository,
                                        MedicalService medicalService,
                                        TrainingService trainingService,
                                        AppointmentReportService appointmentReportService,
                                        Random random) {
        this.documentRepository = documentRepository;
        this.medicalService = medicalService;
        this.trainingService = trainingService;
        this.appointmentReportService = appointmentReportService;
        this.random = random;
    }

    public SimulationResult simulate(UUID customerId, Appointment appointment) {
        return switch (appointment.getType().getSimulationCategory()) {
            case INITIAL_MEDICAL -> simulateInitialMedical(customerId, appointment.getId());
            case FINAL_MEDICAL -> simulateFinalMedical(customerId, appointment.getId());
            case SPECIALIST -> simulateSpecialist(customerId, appointment.getId(), appointment.getType());
            case TRAINING -> simulateSpaceTraining(customerId, appointment.getId());
        };
    }

    private SimulationResult simulateInitialMedical(UUID customerId, UUID appointmentId) {
        Map<SpecialistArea, Boolean> results = new EnumMap<>(SpecialistArea.class);
        for (SpecialistArea area : SpecialistArea.values()) {
            results.put(area, random.nextInt(5) != 0);
        }

        boolean needsExtended = random.nextInt(4) == 0;

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

        return new SimulationResult(true, msg.toString());
    }

    private SimulationResult simulateFinalMedical(UUID customerId, UUID appointmentId) {
        boolean eligible = random.nextInt(20) != 0;
        MedicalReport report = medicalService.recordFinalMedicalResult(
                customerId, appointmentId, eligible, "Auto-generated result.");
        appointmentReportService.createFinalMedicalReport(customerId, report);

        if (eligible) {
            return new SimulationResult(true, "Final medical passed. Customer approved for flight.");
        } else {
            return new SimulationResult(false, "Final medical failed. Offer indemnity agreement.");
        }
    }

    private SimulationResult simulateSpecialist(UUID customerId, UUID appointmentId, AppointmentType type) {
        boolean passed = random.nextInt(10) < 9;
        appointmentReportService.createSpecialistReport(customerId, appointmentId, type, passed);

        if (passed) {
            trainingService.completeSpecialistConsultation(customerId, appointmentId);
            return new SimulationResult(true, type.displayName() + " cleared. Proceed to next step.");
        } else {
            return new SimulationResult(false, type.displayName() + " failed. Offer indemnity agreement.");
        }
    }

    private SimulationResult simulateSpaceTraining(UUID customerId, UUID appointmentId) {
        boolean passed = random.nextInt(10) < 9;
        appointmentReportService.createTrainingReport(customerId, appointmentId, passed);

        if (passed) {
            trainingService.completeSpaceTraining(customerId, appointmentId);
            return new SimulationResult(true, "Space training completed. Ready for final medical.");
        } else {
            return new SimulationResult(false, "Space training failed. Offer indemnity agreement.");
        }
    }
}
