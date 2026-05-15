package de.spacemate.factory;

import de.spacemate.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DefaultAppointmentFactoryTest {

    private final AppointmentFactory factory = new DefaultAppointmentFactory();

    @Test
    void createsAppointmentWithCorrectReferences() {
        Customer customer = new Customer(UUID.randomUUID(), "Test", "User", "t@t.com");
        Staff staff = new Doctor(UUID.randomUUID(), "Dr. Test", StaffRole.CHIEF_PHYSICIAN);
        TimeSlot slot = new TimeSlot(UUID.randomUUID(), staff, LocalDateTime.of(2026, 5, 12, 9, 0),
                LocalDateTime.of(2026, 5, 12, 10, 0));
        UUID id = UUID.randomUUID();
        LocalDateTime scheduledAt = slot.getStart();

        Appointment appointment = factory.create(id, customer, staff, slot,
                AppointmentType.INITIAL_MEDICAL, scheduledAt);

        assertEquals(id, appointment.getId());
        assertSame(customer, appointment.getCustomer());
        assertSame(staff, appointment.getConductor());
        assertSame(slot, appointment.getTimeSlot());
        assertEquals(AppointmentType.INITIAL_MEDICAL, appointment.getType());
        assertEquals(scheduledAt, appointment.getScheduledAt());
    }

    @Test
    void newAppointmentHasSuggestedStatus() {
        Customer customer = new Customer(UUID.randomUUID(), "Test", "User", "t@t.com");
        Staff staff = new Doctor(UUID.randomUUID(), "Dr. Test", StaffRole.CHIEF_PHYSICIAN);
        TimeSlot slot = new TimeSlot(UUID.randomUUID(), staff, LocalDateTime.now(), LocalDateTime.now().plusHours(1));

        Appointment appointment = factory.create(UUID.randomUUID(), customer, staff, slot,
                AppointmentType.INITIAL_MEDICAL, slot.getStart());

        assertEquals(AppointmentStatus.SUGGESTED, appointment.getStatus());
    }
}
