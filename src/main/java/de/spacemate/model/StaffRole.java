package de.spacemate.model;

public enum StaffRole {
    CHIEF_PHYSICIAN("Chief Physician", true),
    RESIDENT_PHYSICIAN("Resident Physician", true),
    NIGHT_PHYSICIAN("Night Physician", true),
    EYE_SPECIALIST("Eye Specialist", false),
    CARDIOLOGIST("Cardiologist", false),
    NEUROLOGIST("Neurologist", false),
    ORTHOPEDIST("Orthopedist", false),
    PSYCHOLOGIST("Psychologist", false),
    SPACE_TRAINER("Space Trainer", false),
    PROCESS_MANAGER("Process Manager", false);

    private final String displayName;
    private final boolean physician;

    StaffRole(String displayName, boolean physician) {
        this.displayName = displayName;
        this.physician = physician;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isPhysician() {
        return physician;
    }
}
