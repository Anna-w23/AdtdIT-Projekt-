package de.spacemate.factory;

import de.spacemate.model.*;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AppointmentFactory {
    Appointment create(UUID id, Customer customer, Staff conductor,
                       TimeSlot timeSlot, AppointmentType type, LocalDateTime scheduledAt);
}
