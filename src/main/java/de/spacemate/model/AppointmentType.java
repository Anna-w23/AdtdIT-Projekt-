package de.spacemate.model;

public enum AppointmentType {
    INITIAL_MEDICAL("Initial Medical", false,
            OnboardingStage.FIRST_MEDICAL_SCHEDULED, OnboardingStage.FIRST_MEDICAL_COMPLETED,
            null, SimulationCategory.INITIAL_MEDICAL, ResourceProfile.MEDICAL),

    EYE_SPECIALIST("Eye Specialist", true,
            OnboardingStage.SPECIALIST_SCHEDULED, OnboardingStage.SPECIALIST_COMPLETED,
            StaffRole.EYE_SPECIALIST, SimulationCategory.SPECIALIST, ResourceProfile.MEDICAL),

    CARDIOLOGIST("Cardiologist", true,
            OnboardingStage.SPECIALIST_SCHEDULED, OnboardingStage.SPECIALIST_COMPLETED,
            StaffRole.CARDIOLOGIST, SimulationCategory.SPECIALIST, ResourceProfile.MEDICAL),

    NEUROLOGIST("Neurologist", true,
            OnboardingStage.SPECIALIST_SCHEDULED, OnboardingStage.SPECIALIST_COMPLETED,
            StaffRole.NEUROLOGIST, SimulationCategory.SPECIALIST, ResourceProfile.MEDICAL),

    ORTHOPEDIST("Orthopedist", true,
            OnboardingStage.SPECIALIST_SCHEDULED, OnboardingStage.SPECIALIST_COMPLETED,
            StaffRole.ORTHOPEDIST, SimulationCategory.SPECIALIST, ResourceProfile.MEDICAL),

    PSYCHOLOGIST_CONSULTATION("Psychologist Consultation", true,
            OnboardingStage.SPECIALIST_SCHEDULED, OnboardingStage.SPECIALIST_COMPLETED,
            StaffRole.PSYCHOLOGIST, SimulationCategory.SPECIALIST, ResourceProfile.MEDICAL),

    SPACE_TRAINING("Space Training", false,
            OnboardingStage.SPACE_TRAINING_SCHEDULED, OnboardingStage.SPACE_TRAINING_COMPLETED,
            StaffRole.SPACE_TRAINER, SimulationCategory.TRAINING, ResourceProfile.TRAINING),

    FINAL_MEDICAL("Final Medical", false,
            OnboardingStage.FINAL_MEDICAL_SCHEDULED, OnboardingStage.FINAL_MEDICAL_COMPLETED,
            null, SimulationCategory.FINAL_MEDICAL, ResourceProfile.MEDICAL);

    private final String displayName;
    private final boolean specialist;
    private final OnboardingStage scheduledStage;
    private final OnboardingStage completedStage;
    private final StaffRole requiredRole;
    private final SimulationCategory simulationCategory;
    private final ResourceProfile resourceProfile;

    AppointmentType(String displayName, boolean specialist,
                    OnboardingStage scheduledStage, OnboardingStage completedStage,
                    StaffRole requiredRole, SimulationCategory simulationCategory,
                    ResourceProfile resourceProfile) {
        this.displayName = displayName;
        this.specialist = specialist;
        this.scheduledStage = scheduledStage;
        this.completedStage = completedStage;
        this.requiredRole = requiredRole;
        this.simulationCategory = simulationCategory;
        this.resourceProfile = resourceProfile;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isSpecialist() {
        return specialist;
    }

    public OnboardingStage scheduledStage() {
        return scheduledStage;
    }

    public OnboardingStage completedStage() {
        return completedStage;
    }

    public StaffRole getRequiredRole() {
        return requiredRole;
    }

    public SimulationCategory getSimulationCategory() {
        return simulationCategory;
    }

    public ResourceProfile getResourceProfile() {
        return resourceProfile;
    }

    public enum SimulationCategory {
        INITIAL_MEDICAL, FINAL_MEDICAL, SPECIALIST, TRAINING
    }

    public enum ResourceProfile {
        MEDICAL, TRAINING
    }
}
