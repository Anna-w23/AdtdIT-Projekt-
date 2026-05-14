package de.spacemate.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class StaffAvailability {

    private final UUID id;
    private final Staff staff;
    private final LocalDateTime start;
    private final LocalDateTime end;

    public StaffAvailability(UUID id, Staff staff, LocalDateTime start, LocalDateTime end) {
        this.id = id;
        this.staff = staff;
        this.start = start;
        this.end = end;
    }

    public UUID getId() { return id; }
    public Staff getStaff() { return staff; }
    public UUID getStaffId() { return staff.getId(); }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }

    public boolean contains(LocalDateTime proposedStart, LocalDateTime proposedEnd) {
        return !proposedStart.isBefore(start) && !proposedEnd.isAfter(end);
    }
}
