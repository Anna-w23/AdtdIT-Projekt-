package de.spacemate.service.mock;

import de.spacemate.model.*;
import de.spacemate.repository.DocumentRepository;
import de.spacemate.service.DocumentAnalysisService;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

public class MockDocumentAnalysisService implements DocumentAnalysisService {

    private final DocumentRepository documentRepository;
    private final Random random;

    public MockDocumentAnalysisService(DocumentRepository documentRepository, Random random) {
        this.documentRepository = documentRepository;
        this.random = random;
    }

    @Override
    public AnalysisResult analyseQuestionnaire(UUID customerId, Document questionnaire) {
        Document legalReport = createAnalysisReport(customerId, DocumentCategory.AI_LEGAL_REPORT,
                "Legal analysis: no contraindications found. Customer eligible for standard onboarding.");

        Document medicalReport = createAnalysisReport(customerId, DocumentCategory.AI_MEDICAL_REPORT,
                "Medical pre-analysis: recommend thorough initial examination. No prior conditions flagged.");

        boolean needsExtendedTraining = random.nextDouble() < 0.25;
        Document trainerReport = createAnalysisReport(customerId, DocumentCategory.AI_TRAINER_REPORT,
                "Trainer analysis: customer fitness profile assessed. Extended training "
                        + (needsExtendedTraining ? "recommended" : "not required") + ".");
        trainerReport.putMetadata("needsExtendedTraining", String.valueOf(needsExtendedTraining));

        return new AnalysisResult(legalReport, medicalReport, trainerReport);
    }

    private Document createAnalysisReport(UUID customerId, DocumentCategory category, String content) {
        Document doc = new Document(UUID.randomUUID(), customerId,
                DocumentType.REPORT, category, DocumentDirection.OUTBOUND,
                LocalDateTime.now(), content);
        documentRepository.save(doc);
        return doc;
    }
}
