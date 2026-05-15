package de.spacemate.factory;

import de.spacemate.model.*;

import java.time.LocalDateTime;
import java.util.UUID;

public class DefaultAppointmentFactory implements AppointmentFactory {

    @Override
    public Appointment create(UUID id, Customer customer, Staff conductor,
                              TimeSlot timeSlot, AppointmentType type, LocalDateTime scheduledAt) {
        return new Appointment(id, customer, conductor, timeSlot, type, scheduledAt);
    }
}
