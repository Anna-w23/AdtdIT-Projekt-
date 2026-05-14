package de.spacemate.model;

import java.time.Duration;

public enum AppointmentType {
    INITIAL_MEDICAL(Duration.ofMinutes(60)),
    EYE_SPECIALIST(Duration.ofMinutes(45)),
    CARDIOLOGIST(Duration.ofMinutes(45)),
    NEUROLOGIST(Duration.ofMinutes(45)),
    ORTHOPEDIST(Duration.ofMinutes(45)),
    PSYCHOLOGIST_CONSULTATION(Duration.ofMinutes(60)),
    SPACE_TRAINING(Duration.ofMinutes(90)),
    FINAL_MEDICAL(Duration.ofMinutes(60));

    private final Duration duration;

    AppointmentType(Duration duration) {
        this.duration = duration;
    }

    public Duration getDuration() {
        return duration;
    }

    public Duration getDuration(boolean extended) {
        if (this == SPACE_TRAINING && extended) {
            return Duration.ofMinutes(120);
        }
        return duration;
    }

    public String displayName() {
        return switch (this) {
            case INITIAL_MEDICAL           -> "Initial Medical";
            case EYE_SPECIALIST            -> "Eye Specialist";
            case CARDIOLOGIST              -> "Cardiologist";
            case NEUROLOGIST               -> "Neurologist";
            case ORTHOPEDIST               -> "Orthopedist";
            case PSYCHOLOGIST_CONSULTATION -> "Psychologist Consultation";
            case SPACE_TRAINING            -> "Space Training";
            case FINAL_MEDICAL             -> "Final Medical";
        };
    }

    public boolean isSpecialist() {
        return switch (this) {
            case EYE_SPECIALIST, CARDIOLOGIST, NEUROLOGIST,
                 ORTHOPEDIST, PSYCHOLOGIST_CONSULTATION -> true;
            default -> false;
        };
    }

    public OnboardingStage scheduledStage() {
        return switch (this) {
            case INITIAL_MEDICAL                                    -> OnboardingStage.FIRST_MEDICAL_SCHEDULED;
            case EYE_SPECIALIST, CARDIOLOGIST, NEUROLOGIST,
                 ORTHOPEDIST, PSYCHOLOGIST_CONSULTATION            -> OnboardingStage.SPECIALIST_SCHEDULED;
            case SPACE_TRAINING                                     -> OnboardingStage.SPACE_TRAINING_SCHEDULED;
            case FINAL_MEDICAL                                      -> OnboardingStage.FINAL_MEDICAL_SCHEDULED;
        };
    }

    public OnboardingStage completedStage() {
        return switch (this) {
            case INITIAL_MEDICAL                                    -> OnboardingStage.FIRST_MEDICAL_COMPLETED;
            case EYE_SPECIALIST, CARDIOLOGIST, NEUROLOGIST,
                 ORTHOPEDIST, PSYCHOLOGIST_CONSULTATION            -> OnboardingStage.SPECIALIST_COMPLETED;
            case SPACE_TRAINING                                     -> OnboardingStage.SPACE_TRAINING_COMPLETED;
            case FINAL_MEDICAL                                      -> OnboardingStage.FINAL_MEDICAL_COMPLETED;
        };
    }
}
