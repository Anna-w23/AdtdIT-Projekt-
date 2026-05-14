package de.spacemate.service.mock;

import de.spacemate.model.*;
import de.spacemate.repository.DocumentRepository;
import de.spacemate.service.AppointmentReportService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class MockAppointmentReportService implements AppointmentReportService {

    private final DocumentRepository documentRepository;

    public MockAppointmentReportService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Override
    public Document createInitialMedicalReport(UUID customerId, MedicalReport medicalReport) {
        StringBuilder content = new StringBuilder();
        content.append("INITIAL MEDICAL EXAMINATION REPORT\n\n");
        content.append("Specialist Area Results:\n");

        Map<SpecialistArea, Boolean> results = medicalReport.getSpecialistResults();
        for (SpecialistArea area : SpecialistArea.values()) {
            Boolean passed = results.get(area);
            String status = (passed != null && passed) ? "PASSED" : "FAILED";
            content.append("  • ").append(area.displayName()).append(": ").append(status).append("\n");
        }

        content.append("\nExtended Training Required: ")
                .append(medicalReport.isNeedsExtendedTraining() ? "Yes" : "No");

        if (!medicalReport.getFailedAreas().isEmpty()) {
            content.append("\n\nFollow-up required: ");
            content.append(medicalReport.getFailedAreas().stream()
                    .map(SpecialistArea::displayName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
        }

        String outcome = medicalReport.requiresSpecialists() ? "REQUIRES_FOLLOWUP" : "PASSED";

        Document doc = new Document(UUID.randomUUID(), customerId,
                DocumentType.REPORT, DocumentCategory.MEDICAL_REPORT,
                DocumentDirection.INBOUND, LocalDateTime.now(), content.toString());
        doc.putMetadata("appointmentId", medicalReport.getAppointmentId().toString());
        doc.putMetadata("outcome", outcome);
        documentRepository.save(doc);
        return doc;
    }

    @Override
    public Document createSpecialistReport(UUID customerId, UUID appointmentId,
                                           AppointmentType type, boolean passed) {
        String content = "SPECIALIST CONSULTATION REPORT\n\n"
                + "Type: " + type.displayName() + "\n"
                + "Result: " + (passed ? "PASSED — Patient cleared." : "FAILED — Patient not cleared.")
                + "\n\nRecommendation: "
                + (passed ? "Proceed with onboarding." : "Consider indemnity agreement or alternative pathway.");

        Document doc = new Document(UUID.randomUUID(), customerId,
                DocumentType.REPORT, DocumentCategory.SPECIALIST_REPORT,
                DocumentDirection.INBOUND, LocalDateTime.now(), content);
        doc.putMetadata("appointmentId", appointmentId.toString());
        doc.putMetadata("outcome", passed ? "PASSED" : "FAILED");
        documentRepository.save(doc);
        return doc;
    }

    @Override
    public Document createTrainingReport(UUID customerId, UUID appointmentId, boolean passed) {
        String content = "SPACE TRAINING REPORT\n\n"
                + "Result: " + (passed ? "PASSED" : "FAILED") + "\n\n"
                + (passed
                ? "Trainee demonstrated adequate competence in zero-gravity procedures and emergency protocols. Cleared for final medical."
                : "Trainee did not meet minimum competence thresholds. Consider indemnity agreement or re-scheduling.");

        Document doc = new Document(UUID.randomUUID(), customerId,
                DocumentType.REPORT, DocumentCategory.TRAINING_REPORT,
                DocumentDirection.INBOUND, LocalDateTime.now(), content);
        doc.putMetadata("appointmentId", appointmentId.toString());
        doc.putMetadata("outcome", passed ? "PASSED" : "FAILED");
        documentRepository.save(doc);
        return doc;
    }

    @Override
    public Document createFinalMedicalReport(UUID customerId, MedicalReport medicalReport) {
        String outcome = medicalReport.isFlightEligible() ? "APPROVED" : "NOT_ELIGIBLE";
        String content = "FINAL MEDICAL EXAMINATION REPORT\n\n"
                + "Flight Eligibility: " + (medicalReport.isFlightEligible() ? "APPROVED" : "NOT ELIGIBLE") + "\n\n"
                + (medicalReport.isFlightEligible()
                ? "Patient meets all medical requirements for spaceflight. Cleared for departure."
                : "Patient does not meet flight eligibility criteria. Consider indemnity agreement.");

        Document doc = new Document(UUID.randomUUID(), customerId,
                DocumentType.REPORT, DocumentCategory.MEDICAL_REPORT,
                DocumentDirection.INBOUND, LocalDateTime.now(), content);
        doc.putMetadata("appointmentId", medicalReport.getAppointmentId().toString());
        doc.putMetadata("outcome", outcome);
        documentRepository.save(doc);
        return doc;
    }
}
