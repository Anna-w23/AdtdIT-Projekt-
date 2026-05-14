package de.spacemate.model;

public enum SpecialistArea {
    EYES,
    CARDIOLOGY,
    NEUROLOGY,
    ORTHOPEDICS,
    PSYCHOLOGY;

    public AppointmentType toAppointmentType() {
        return switch (this) {
            case EYES -> AppointmentType.EYE_SPECIALIST;
            case CARDIOLOGY -> AppointmentType.CARDIOLOGIST;
            case NEUROLOGY -> AppointmentType.NEUROLOGIST;
            case ORTHOPEDICS -> AppointmentType.ORTHOPEDIST;
            case PSYCHOLOGY -> AppointmentType.PSYCHOLOGIST_CONSULTATION;
        };
    }

    public String displayName() {
        return switch (this) {
            case EYES -> "Eyes";
            case CARDIOLOGY -> "Cardiology";
            case NEUROLOGY -> "Neurology";
            case ORTHOPEDICS -> "Orthopedics";
            case PSYCHOLOGY -> "Psychology";
        };
    }
}
