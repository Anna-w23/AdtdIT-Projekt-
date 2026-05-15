package de.spacemate.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Appointment {

    private final UUID id;
    private final Customer customer;
    private final Staff conductor;
    private final TimeSlot timeSlot;
    private final AppointmentType type;
    private AppointmentStatus status;
    private final LocalDateTime scheduledAt;

    public Appointment(UUID id, Customer customer, Staff conductor,
                       TimeSlot timeSlot, AppointmentType type, LocalDateTime scheduledAt) {
        this.id = id;
        this.customer = customer;
        this.conductor = conductor;
        this.timeSlot = timeSlot;
        this.type = type;
        this.scheduledAt = scheduledAt;
        this.status = AppointmentStatus.SUGGESTED;
    }

    public UUID getId() { return id; }
    public Customer getCustomer() { return customer; }
    public UUID getCustomerId() { return customer.getId(); }
    public Staff getConductor() { return conductor; }
    public UUID getStaffId() { return conductor.getId(); }
    public TimeSlot getTimeSlot() { return timeSlot; }
    public UUID getTimeSlotId() { return timeSlot.getId(); }
    public AppointmentType getType() { return type; }
    public AppointmentStatus getStatus() { return status; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }

    public void setStatus(AppointmentStatus status) { this.status = status; }

    @Override
    public String toString() {
        return type + " @ " + scheduledAt + " [" + status + "]";
    }
}
