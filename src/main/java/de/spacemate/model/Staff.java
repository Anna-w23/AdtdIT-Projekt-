package de.spacemate.model;

import java.util.List;
import java.util.UUID;

public interface Staff {
    UUID getId();
    String getName();
    StaffRole getRole();
    List<TimeSlot> getAvailability();
    void addTimeSlot(TimeSlot slot);
}
