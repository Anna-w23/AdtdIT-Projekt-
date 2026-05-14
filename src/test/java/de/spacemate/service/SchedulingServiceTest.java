package de.spacemate.service;

import de.spacemate.factory.AppointmentFactory;
import de.spacemate.factory.DefaultAppointmentFactory;
import de.spacemate.factory.DefaultTimeSlotFactory;
import de.spacemate.factory.TimeSlotFactory;
import de.spacemate.model.*;
import de.spacemate.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SchedulingServiceTest {

    private final UUID customerId = UUID.randomUUID();
    private final UUID staffId = UUID.randomUUID();
    private final UUID slotId = UUID.randomUUID();

    private final Customer customer = new Customer(customerId, "Test", "User", "t@t.com");
    private final Staff doctor = new Doctor(staffId, "Dr. Test", StaffRole.CHIEF_PHYSICIAN);
    private final LocalDateTime slotStart = LocalDateTime.of(2026, 5, 15, 9, 0);
    private final TimeSlot slot = new TimeSlot(slotId, doctor, slotStart, slotStart.plusHours(1));

    private final List<Appointment> savedAppointments = new ArrayList<>();
    private boolean slotMarkedBooked = false;

    private final CustomerRepository customerRepo = new CustomerRepository() {
        @Override public void save(Customer c) {}
        @Override public Optional<Customer> findById(UUID id) {
            return id.equals(customerId) ? Optional.of(customer) : Optional.empty();
        }
        @Override public List<Customer> findAll() { return List.of(customer); }
        @Override public List<Customer> findByStage(OnboardingStage stage) { return List.of(); }
        @Override public void delete(UUID id) {}
    };

    private final TimeSlotRepository timeSlotRepo = new TimeSlotRepository() {
        @Override public void save(TimeSlot ts) {}
        @Override public Optional<TimeSlot> findById(UUID id) {
            return id.equals(slotId) ? Optional.of(slot) : Optional.empty();
        }
        @Override public List<TimeSlot> findAvailableByStaffId(UUID id) {
            return id.equals(staffId) ? List.of(slot) : List.of();
        }
        @Override public List<TimeSlot> findBookedByStaffIdAndDate(UUID id, LocalDate date) {
            return List.of();
        }
        @Override public void markBooked(UUID id) { slotMarkedBooked = true; }
        @Override public void markFree(UUID id) {}
    };

    private final AppointmentRepository appointmentRepo = new AppointmentRepository() {
        @Override public void save(Appointment a) { savedAppointments.add(a); }
        @Override public Optional<Appointment> findById(UUID id) { return Optional.empty(); }
        @Override public List<Appointment> findByCustomerId(UUID id) { return List.of(); }
        @Override public List<Appointment> findByCustomerIdAndType(UUID id, AppointmentType type) { return List.of(); }
        @Override public List<Appointment> findByDateAndStatus(LocalDate date, AppointmentStatus status) { return List.of(); }
        @Override public List<Appointment> findAll() { return new ArrayList<>(savedAppointments); }
    };

    private final CustomerAvailabilityRepository availabilityRepo = new CustomerAvailabilityRepository() {
        @Override public void setAvailability(UUID id, Set<LocalDateTime> times) {}
        @Override public Set<LocalDateTime> getAvailability(UUID id) {
            return Set.of(slotStart);
        }
        @Override public void removeSlot(UUID id, LocalDateTime slot) {}
    };

    private final StaffAvailabilityRepository staffAvailabilityRepo = new StaffAvailabilityRepository() {
        @Override public void save(StaffAvailability sa) {}
        @Override public List<StaffAvailability> findByStaffId(UUID id) { return List.of(); }
        @Override public List<StaffAvailability> findByStaffIdAndDate(UUID id, LocalDate date) {
            return List.of(new StaffAvailability(UUID.randomUUID(), doctor,
                    LocalDateTime.of(2026, 5, 15, 8, 0), LocalDateTime.of(2026, 5, 15, 17, 0)));
        }
    };

    private final StaffRepository staffRepo = new StaffRepository() {
        @Override public void save(Staff s) {}
        @Override public Optional<Staff> findById(UUID id) {
            return id.equals(staffId) ? Optional.of(doctor) : Optional.empty();
        }
        @Override public List<Staff> findAll() { return List.of(doctor); }
        @Override public List<Staff> findByRole(StaffRole role) { return List.of(doctor); }
    };

    private final AppointmentTypeStaffResolver staffResolver = type -> List.of(doctor);
    private final AppointmentFactory appointmentFactory = new DefaultAppointmentFactory();
    private final TimeSlotFactory timeSlotFactory = new DefaultTimeSlotFactory();

    private SchedulingService service;

    @BeforeEach
    void setUp() {
        service = new SchedulingService(customerRepo, timeSlotRepo, appointmentRepo,
                availabilityRepo, staffAvailabilityRepo, staffRepo,
                staffResolver, appointmentFactory, timeSlotFactory);
    }

    @Test
    void scheduleAppointmentCreatesAndSavesAppointment() {
        Appointment result = service.scheduleAppointment(customerId, staffId, slotId, AppointmentType.INITIAL_MEDICAL);

        assertNotNull(result);
        assertEquals(AppointmentType.INITIAL_MEDICAL, result.getType());
        assertSame(customer, result.getCustomer());
        assertSame(doctor, result.getConductor());
        assertEquals(1, savedAppointments.size());
    }

    @Test
    void scheduleAppointmentMarksSlotAsBooked() {
        service.scheduleAppointment(customerId, staffId, slotId, AppointmentType.INITIAL_MEDICAL);
        assertTrue(slotMarkedBooked);
    }

    @Test
    void scheduleAppointmentUpdatesCustomerStage() {
        service.scheduleAppointment(customerId, staffId, slotId, AppointmentType.INITIAL_MEDICAL);
        assertEquals(OnboardingStage.FIRST_MEDICAL_SCHEDULED, customer.getCurrentStage());
    }

    @Test
    void scheduleAppointmentThrowsWhenCustomerNotAvailable() {
        CustomerAvailabilityRepository restrictedAvail = new CustomerAvailabilityRepository() {
            @Override public void setAvailability(UUID id, Set<LocalDateTime> times) {}
            @Override public Set<LocalDateTime> getAvailability(UUID id) {
                return Set.of(LocalDateTime.of(2026, 5, 15, 14, 0));
            }
            @Override public void removeSlot(UUID id, LocalDateTime slot) {}
        };

        SchedulingService restrictedService = new SchedulingService(customerRepo, timeSlotRepo,
                appointmentRepo, restrictedAvail, staffAvailabilityRepo, staffRepo,
                staffResolver, appointmentFactory, timeSlotFactory);

        assertThrows(CustomerNotAvailableException.class,
                () -> restrictedService.scheduleAppointment(customerId, staffId, slotId, AppointmentType.INITIAL_MEDICAL));
    }

    @Test
    void findAvailableSlotsReturnsStaffSlots() {
        List<TimeSlot> result = service.findAvailableSlots(AppointmentType.INITIAL_MEDICAL);
        assertEquals(1, result.size());
        assertSame(slot, result.getFirst());
    }

    @Test
    void scheduleByDragCreatesAppointmentWithinWindow() {
        LocalDateTime start = LocalDateTime.of(2026, 5, 15, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 5, 15, 10, 0);

        CustomerAvailabilityRepository fullDayAvail = new CustomerAvailabilityRepository() {
            @Override public void setAvailability(UUID id, Set<LocalDateTime> times) {}
            @Override public Set<LocalDateTime> getAvailability(UUID id) {
                return Set.of(start, start.plusMinutes(30));
            }
            @Override public void removeSlot(UUID id, LocalDateTime slot) {}
        };

        SchedulingService svc = new SchedulingService(customerRepo, timeSlotRepo,
                appointmentRepo, fullDayAvail, staffAvailabilityRepo, staffRepo,
                staffResolver, appointmentFactory, timeSlotFactory);

        Appointment result = svc.scheduleByDrag(customerId, staffId, start, end, AppointmentType.INITIAL_MEDICAL);

        assertNotNull(result);
        assertEquals(AppointmentType.INITIAL_MEDICAL, result.getType());
        assertEquals(1, savedAppointments.size());
    }

    @Test
    void scheduleByDragThrowsWhenOutsideStaffWindow() {
        LocalDateTime start = LocalDateTime.of(2026, 5, 15, 18, 0);
        LocalDateTime end = LocalDateTime.of(2026, 5, 15, 19, 0);

        CustomerAvailabilityRepository fullDayAvail = new CustomerAvailabilityRepository() {
            @Override public void setAvailability(UUID id, Set<LocalDateTime> times) {}
            @Override public Set<LocalDateTime> getAvailability(UUID id) {
                return Set.of(start, start.plusMinutes(30));
            }
            @Override public void removeSlot(UUID id, LocalDateTime slot) {}
        };

        StaffAvailabilityRepository emptyStaffAvail = new StaffAvailabilityRepository() {
            @Override public void save(StaffAvailability sa) {}
            @Override public List<StaffAvailability> findByStaffId(UUID id) { return List.of(); }
            @Override public List<StaffAvailability> findByStaffIdAndDate(UUID id, LocalDate date) { return List.of(); }
        };

        SchedulingService svc = new SchedulingService(customerRepo, timeSlotRepo,
                appointmentRepo, fullDayAvail, emptyStaffAvail, staffRepo,
                staffResolver, appointmentFactory, timeSlotFactory);

        assertThrows(IllegalArgumentException.class,
                () -> svc.scheduleByDrag(customerId, staffId, start, end, AppointmentType.INITIAL_MEDICAL));
    }
}
