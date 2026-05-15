package de.spacemate.model;

public enum SpecialistArea {
    EYES("Eyes", AppointmentType.EYE_SPECIALIST),
    CARDIOLOGY("Cardiology", AppointmentType.CARDIOLOGIST),
    NEUROLOGY("Neurology", AppointmentType.NEUROLOGIST),
    ORTHOPEDICS("Orthopedics", AppointmentType.ORTHOPEDIST),
    PSYCHOLOGY("Psychology", AppointmentType.PSYCHOLOGIST_CONSULTATION);

    private final String displayName;
    private final AppointmentType appointmentType;

    SpecialistArea(String displayName, AppointmentType appointmentType) {
        this.displayName = displayName;
        this.appointmentType = appointmentType;
    }

    public AppointmentType toAppointmentType() {
        return appointmentType;
    }

    public String displayName() {
        return displayName;
    }
}
