package de.spacemate.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class TimeSlot {

    private final UUID id;
    private final Staff staff;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private boolean booked;

    public TimeSlot(UUID id, Staff staff, LocalDateTime start, LocalDateTime end) {
        this.id = id;
        this.staff = staff;
        this.start = start;
        this.end = end;
        this.booked = false;
    }

    public UUID getId() { return id; }
    public Staff getStaff() { return staff; }
    public UUID getStaffId() { return staff.getId(); }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }
    public boolean isBooked() { return booked; }

    public void setBooked(boolean booked) { this.booked = booked; }

    @Override
    public String toString() {
        return start + " – " + end + " (" + (booked ? "booked" : "free") + ")";
    }
}
