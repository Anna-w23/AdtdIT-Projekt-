package de.spacemate.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AbstractStaff implements Staff {

    private final UUID id;
    private final String name;
    private final StaffRole role;
    private final List<TimeSlot> availability;

    protected AbstractStaff(UUID id, String name, StaffRole role) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.availability = new ArrayList<>();
    }

    @Override
    public UUID getId() { return id; }

    @Override
    public String getName() { return name; }

    @Override
    public StaffRole getRole() { return role; }

    @Override
    public List<TimeSlot> getAvailability() { return availability; }

    @Override
    public void addTimeSlot(TimeSlot slot) {
        availability.add(slot);
    }

    @Override
    public String toString() {
        return name + " (" + role + ")";
    }
}
