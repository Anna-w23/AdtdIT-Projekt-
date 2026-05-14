package de.spacemate.service.mock;

import de.spacemate.model.*;
import de.spacemate.repository.DocumentRepository;
import de.spacemate.service.DocumentAnalysisService;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

public class MockDocumentAnalysisService implements DocumentAnalysisService {

    private final DocumentRepository documentRepository;
    private final Random random = new Random();

    public MockDocumentAnalysisService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Override
    public AnalysisResult analyseQuestionnaire(UUID customerId, Document questionnaire) {
        LocalDateTime now = LocalDateTime.now();

        Document legalReport = new Document(UUID.randomUUID(), customerId,
                DocumentType.REPORT, DocumentCategory.AI_LEGAL_REPORT,
                DocumentDirection.OUTBOUND, now,
                "Legal analysis: no contraindications found. Customer eligible for standard onboarding.");
        documentRepository.save(legalReport);

        Document medicalReport = new Document(UUID.randomUUID(), customerId,
                DocumentType.REPORT, DocumentCategory.AI_MEDICAL_REPORT,
                DocumentDirection.OUTBOUND, now,
                "Medical pre-analysis: recommend thorough initial examination. No prior conditions flagged.");
        documentRepository.save(medicalReport);

        boolean needsExtendedTraining = random.nextDouble() < 0.25;
        Document trainerReport = new Document(UUID.randomUUID(), customerId,
                DocumentType.REPORT, DocumentCategory.AI_TRAINER_REPORT,
                DocumentDirection.OUTBOUND, now,
                "Trainer analysis: customer fitness profile assessed. Extended training "
                        + (needsExtendedTraining ? "recommended" : "not required") + ".");
        trainerReport.putMetadata("needsExtendedTraining", String.valueOf(needsExtendedTraining));
        documentRepository.save(trainerReport);

        return new AnalysisResult(legalReport, medicalReport, trainerReport);
    }
}
