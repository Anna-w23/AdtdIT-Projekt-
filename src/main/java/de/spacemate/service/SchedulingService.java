package de.spacemate.service;

import de.spacemate.factory.AppointmentFactory;
import de.spacemate.factory.TimeSlotFactory;
import de.spacemate.model.*;
import de.spacemate.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SchedulingService {

    private final CustomerRepository customerRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final AppointmentRepository appointmentRepository;
    private final CustomerAvailabilityRepository availabilityRepository;
    private final StaffAvailabilityRepository staffAvailabilityRepository;
    private final StaffRepository staffRepository;
    private final AppointmentTypeStaffResolver staffResolver;
    private final AppointmentFactory appointmentFactory;
    private final TimeSlotFactory timeSlotFactory;

    public SchedulingService(CustomerRepository customerRepository,
                             TimeSlotRepository timeSlotRepository,
                             AppointmentRepository appointmentRepository,
                             CustomerAvailabilityRepository availabilityRepository,
                             StaffAvailabilityRepository staffAvailabilityRepository,
                             StaffRepository staffRepository,
                             AppointmentTypeStaffResolver staffResolver,
                             AppointmentFactory appointmentFactory,
                             TimeSlotFactory timeSlotFactory) {
        this.customerRepository = customerRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.appointmentRepository = appointmentRepository;
        this.availabilityRepository = availabilityRepository;
        this.staffAvailabilityRepository = staffAvailabilityRepository;
        this.staffRepository = staffRepository;
        this.staffResolver = staffResolver;
        this.appointmentFactory = appointmentFactory;
        this.timeSlotFactory = timeSlotFactory;
    }

    public List<TimeSlot> findAvailableSlots(AppointmentType type) {
        return staffResolver.findQualifiedStaff(type).stream()
                .flatMap(staff -> timeSlotRepository.findAvailableByStaffId(staff.getId()).stream())
                .toList();
    }

    public Appointment scheduleAppointment(UUID customerId, UUID staffId,
                                           UUID slotId, AppointmentType type) {
        Customer customer = getCustomer(customerId);
        TimeSlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("TimeSlot not found: " + slotId));

        Set<LocalDateTime> customerAvail = availabilityRepository.getAvailability(customerId);
        if (!customerAvail.isEmpty() && !customerAvail.contains(slot.getStart())) {
            throw new CustomerNotAvailableException(customer.getFullName(), slot.getStart());
        }

        timeSlotRepository.markBooked(slotId);

        Appointment appointment = appointmentFactory.create(
                UUID.randomUUID(), customer, slot.getStaff(), slot, type, slot.getStart());
        appointmentRepository.save(appointment);

        OnboardingStage nextStage = type.scheduledStage();
        customer.setCurrentStage(nextStage);
        customerRepository.save(customer);

        return appointment;
    }

    public Appointment scheduleByDrag(UUID customerId, UUID staffId,
                                      LocalDateTime start, LocalDateTime end,
                                      AppointmentType type) {
        Customer customer = getCustomer(customerId);

        Set<LocalDateTime> customerAvail = availabilityRepository.getAvailability(customerId);
        if (!customerAvail.isEmpty()) {
            LocalDateTime check = start;
            while (check.isBefore(end)) {
                if (!customerAvail.contains(check)) {
                    throw new CustomerNotAvailableException(customer.getFullName(), check);
                }
                check = check.plusMinutes(30);
            }
        }

        List<StaffAvailability> windows = staffAvailabilityRepository
                .findByStaffIdAndDate(staffId, start.toLocalDate());
        boolean withinWindow = windows.stream()
                .anyMatch(w -> w.contains(start, end));
        if (!withinWindow) {
            throw new IllegalArgumentException("Proposed time is outside staff availability");
        }

        List<TimeSlot> bookedSlots = timeSlotRepository
                .findBookedByStaffIdAndDate(staffId, start.toLocalDate());
        boolean overlaps = bookedSlots.stream().anyMatch(existing ->
                start.isBefore(existing.getEnd()) && end.isAfter(existing.getStart()));
        if (overlaps) {
            throw new IllegalStateException("Proposed time overlaps with existing appointment");
        }

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));
        TimeSlot newSlot = timeSlotFactory.create(UUID.randomUUID(), staff, start, end);
        newSlot.setBooked(true);
        timeSlotRepository.save(newSlot);

        Appointment appointment = appointmentFactory.create(
                UUID.randomUUID(), customer, staff, newSlot, type, start);
        appointmentRepository.save(appointment);

        return appointment;
    }

    public void confirmAppointment(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.save(appointment);
    }

    public void markSent(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
        appointment.setStatus(AppointmentStatus.SENT);
        appointmentRepository.save(appointment);
    }

    public void cancelAppointment(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
        appointment.setStatus(AppointmentStatus.CANCELLED);
        timeSlotRepository.markFree(appointment.getTimeSlotId());
        appointmentRepository.save(appointment);
    }

    public List<Appointment> getAppointmentsForCustomer(UUID customerId) {
        return appointmentRepository.findByCustomerId(customerId);
    }

    public List<TimeSlot> findAvailableSlotsByStaff(UUID staffId) {
        return timeSlotRepository.findAvailableByStaffId(staffId);
    }

    public List<StaffAvailability> getStaffAvailability(UUID staffId) {
        return staffAvailabilityRepository.findByStaffId(staffId);
    }

    public List<TimeSlot> getBookedSlotsByStaffAndDate(UUID staffId, LocalDate date) {
        return timeSlotRepository.findBookedByStaffIdAndDate(staffId, date);
    }

    public Set<LocalDateTime> getCustomerAvailability(UUID customerId) {
        return availabilityRepository.getAvailability(customerId);
    }

    public void removeCustomerTimeslot(UUID customerId, LocalDateTime slotStart, LocalDateTime slotEnd) {
        LocalDateTime t = slotStart;
        while (t.isBefore(slotEnd)) {
            availabilityRepository.removeSlot(customerId, t);
            t = t.plusMinutes(30);
        }
    }

    private Customer getCustomer(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
    }
}
