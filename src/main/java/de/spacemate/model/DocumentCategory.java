package de.spacemate.model;

public enum DocumentCategory {
    QUESTIONNAIRE("Questionnaire"),
    AI_LEGAL_REPORT("AI Legal Report"),
    AI_MEDICAL_REPORT("AI Medical Report"),
    AI_TRAINER_REPORT("AI Trainer Report"),
    MEDICAL_REPORT("Medical Report"),
    SPECIALIST_REPORT("Specialist Report"),
    TRAINING_REPORT("Training Report"),
    APPOINTMENT_PROPOSAL("Appointment Proposal"),
    APPOINTMENT_RESPONSE("Appointment Response"),
    INDEMNITY_AGREEMENT("Indemnity Agreement"),
    INDEMNITY_RESPONSE("Indemnity Response");

    private final String displayName;

    DocumentCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
