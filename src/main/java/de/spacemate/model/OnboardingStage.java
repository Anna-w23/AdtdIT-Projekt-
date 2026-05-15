package de.spacemate.model;

public enum OnboardingStage {
    REGISTERED("Registered"),
    QUESTIONNAIRE_SENT("Questionnaire Sent"),
    QUESTIONNAIRE_COMPLETED("Questionnaire Completed"),
    INDEMNITY_PENDING("Indemnity Pending"),
    INDEMNITY_SIGNED("Indemnity Signed"),
    FIRST_MEDICAL_SCHEDULED("Initial Medical Scheduled"),
    FIRST_MEDICAL_COMPLETED("Initial Medical Completed"),
    SPECIALIST_SCHEDULED("Specialist Scheduled"),
    SPECIALIST_COMPLETED("Specialist Completed"),
    SPACE_TRAINING_SCHEDULED("Space Training Scheduled"),
    SPACE_TRAINING_COMPLETED("Space Training Completed"),
    FINAL_MEDICAL_SCHEDULED("Final Medical Scheduled"),
    FINAL_MEDICAL_COMPLETED("Final Medical Completed"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    FAILED("Failed"),
    APPOINTMENT_REFUSED("Appointment Refused");

    private final String displayName;
    private OnboardingStage rollbackTo;

    OnboardingStage(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public OnboardingStage rollbackStage() {
        return rollbackTo != null ? rollbackTo : this;
    }

    static {
        FIRST_MEDICAL_SCHEDULED.rollbackTo = QUESTIONNAIRE_COMPLETED;
        SPECIALIST_SCHEDULED.rollbackTo = FIRST_MEDICAL_COMPLETED;
        SPACE_TRAINING_SCHEDULED.rollbackTo = SPECIALIST_COMPLETED;
        FINAL_MEDICAL_SCHEDULED.rollbackTo = SPACE_TRAINING_COMPLETED;
    }
}
