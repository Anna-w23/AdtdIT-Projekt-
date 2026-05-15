package de.spacemate.model;

import java.time.LocalDate;
import java.util.*;

public class MedicalReport {

    private final UUID id;
    private final Customer customer;
    private final Appointment appointment;
    private final LocalDate issuedOn;

    private final Map<SpecialistArea, Boolean> specialistResults;
    private boolean needsExtendedTraining;
    private boolean flightEligible;
    private String remarks;

    public MedicalReport(UUID id, Customer customer, Appointment appointment, LocalDate issuedOn) {
        this.id = id;
        this.customer = customer;
        this.appointment = appointment;
        this.issuedOn = issuedOn;
        this.specialistResults = new EnumMap<>(SpecialistArea.class);
        this.needsExtendedTraining = false;
        this.flightEligible = true;
    }

    public UUID getId() { return id; }
    public Customer getCustomer() { return customer; }
    public UUID getCustomerId() { return customer.getId(); }
    public Appointment getAppointment() { return appointment; }
    public UUID getAppointmentId() { return appointment.getId(); }
    public LocalDate getIssuedOn() { return issuedOn; }
    public boolean isNeedsExtendedTraining() { return needsExtendedTraining; }
    public boolean isFlightEligible() { return flightEligible; }
    public String getRemarks() { return remarks; }
    public Map<SpecialistArea, Boolean> getSpecialistResults() { return Collections.unmodifiableMap(specialistResults); }

    public void setSpecialistResult(SpecialistArea area, boolean passed) {
        specialistResults.put(area, passed);
    }

    public void setNeedsExtendedTraining(boolean needsExtendedTraining) {
        this.needsExtendedTraining = needsExtendedTraining;
    }

    public void setFlightEligible(boolean flightEligible) {
        this.flightEligible = flightEligible;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public List<SpecialistArea> getFailedAreas() {
        return specialistResults.entrySet().stream()
                .filter(e -> !e.getValue())
                .map(Map.Entry::getKey)
                .toList();
    }

    public List<AppointmentType> getRequiredSpecialists() {
        return getFailedAreas().stream()
                .map(SpecialistArea::toAppointmentType)
                .toList();
    }

    public boolean requiresSpecialists() {
        return !getFailedAreas().isEmpty();
    }
}
