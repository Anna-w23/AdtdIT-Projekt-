package de.spacemate.service;

import de.spacemate.model.*;
import de.spacemate.repository.AppointmentRepository;
import de.spacemate.repository.CustomerRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public class RefusalHandler {

    private final CustomerRepository customerRepository;
    private final AppointmentRepository appointmentRepository;
    private final SchedulingService schedulingService;

    public RefusalHandler(CustomerRepository customerRepository,
                          AppointmentRepository appointmentRepository,
                          SchedulingService schedulingService) {
        this.customerRepository = customerRepository;
        this.appointmentRepository = appointmentRepository;
        this.schedulingService = schedulingService;
    }

    public RefusalResult refuseTimeslot(UUID customerId, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));

        LocalDateTime slotStart = appointment.getTimeSlot().getStart();
        LocalDateTime slotEnd = appointment.getTimeSlot().getEnd();

        schedulingService.cancelAppointment(appointmentId);
        schedulingService.removeCustomerTimeslot(customerId, slotStart, slotEnd);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        OnboardingStage rolledBack = rollbackStage(appointment.getType().scheduledStage());
        customer.setCurrentStage(rolledBack);
        customer.setNeedsAttention(true,
                "Customer disagrees with timeslot for " + appointment.getType().displayName() + " – reschedule required");
        customerRepository.save(customer);

        return new RefusalResult(customer,
                "Customer disagreed with timeslot. Reschedule " + appointment.getType().displayName() + ".");
    }

    public RefusalResult refuseAppointmentType(UUID customerId, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));

        schedulingService.cancelAppointment(appointmentId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        customer.setCurrentStage(OnboardingStage.APPOINTMENT_REFUSED);
        customer.setNeedsAttention(true,
                "Customer refused " + appointment.getType().displayName() + " – offer indemnity agreement");
        customerRepository.save(customer);

        return new RefusalResult(customer,
                "Customer refused " + appointment.getType().displayName() + ". Offer indemnity agreement.");
    }

    public static OnboardingStage rollbackStage(OnboardingStage scheduled) {
        return scheduled.rollbackStage();
    }

    public record RefusalResult(Customer customer, String message) {}
}
