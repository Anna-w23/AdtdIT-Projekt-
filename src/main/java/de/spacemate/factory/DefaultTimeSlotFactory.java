package de.spacemate.factory;

import de.spacemate.model.Staff;
import de.spacemate.model.TimeSlot;

import java.time.LocalDateTime;
import java.util.UUID;

public class DefaultTimeSlotFactory implements TimeSlotFactory {

    @Override
    public TimeSlot create(UUID id, Staff staff, LocalDateTime start, LocalDateTime end) {
        return new TimeSlot(id, staff, start, end);
    }
}
