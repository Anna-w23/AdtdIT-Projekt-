package de.spacemate.service;

import de.spacemate.model.AnalysisResult;
import de.spacemate.model.Document;

import java.util.UUID;

public interface DocumentAnalysisService {
    AnalysisResult analyseQuestionnaire(UUID customerId, Document questionnaire);
}
