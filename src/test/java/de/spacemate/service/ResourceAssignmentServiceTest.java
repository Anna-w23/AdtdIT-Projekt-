package de.spacemate.service;

import de.spacemate.model.*;
import de.spacemate.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ResourceAssignmentServiceTest {

    private final UUID staffId = UUID.randomUUID();
    private final UUID roomId = UUID.randomUUID();
    private final UUID overflowRoomId = UUID.randomUUID();
    private final UUID vrHeadsetId = UUID.randomUUID();
    private final UUID appointmentId = UUID.randomUUID();

    private final LocalDateTime start = LocalDateTime.of(2026, 5, 15, 9, 0);
    private final LocalDateTime end = LocalDateTime.of(2026, 5, 15, 10, 0);

    private final Resource room = new Room(roomId, "Dr. Richter's Office", "MEDICAL_ROOM");
    private final Resource overflowRoom = new Room(overflowRoomId, "Overflow Room 1", "OVERFLOW_ROOM");
    private final Resource vrHeadset = new Equipment(vrHeadsetId, "VR Headset #1", "VR_HEADSET");

    private final Customer customer = new Customer(UUID.randomUUID(), "Max", "Mustermann", "m@m.com");
    private final Staff doctor = new Doctor(staffId, "Dr. Richter", StaffRole.CHIEF_PHYSICIAN);
    private final TimeSlot slot = new TimeSlot(UUID.randomUUID(), doctor, start, end);
    private final Appointment appointment = new Appointment(appointmentId, customer, doctor,
            slot, AppointmentType.INITIAL_MEDICAL, start);

    private final List<ResourceAssignment> savedAssignments = new ArrayList<>();
    private final Set<String> deletedAssignments = new HashSet<>();

    private final ResourceRepository resourceRepo = new ResourceRepository() {
        private final List<Resource> all = List.of(room, overflowRoom, vrHeadset);
        @Override public void save(Resource r) {}
        @Override public Optional<Resource> findById(UUID id) {
            if (id.equals(roomId)) return Optional.of(room);
            if (id.equals(overflowRoomId)) return Optional.of(overflowRoom);
            if (id.equals(vrHeadsetId)) return Optional.of(vrHeadset);
            return Optional.empty();
        }
        @Override public List<Resource> findByCategory(ResourceCategory cat) {
            return all.stream().filter(r -> r.getCategory() == cat).toList();
        }
        @Override public List<Resource> findByCategoryAndTag(ResourceCategory cat, String tag) {
            return all.stream()
                    .filter(r -> r.getCategory() == cat && Objects.equals(r.getTag(), tag))
                    .toList();
        }
    };

    private final ResourceAvailabilityRepository availabilityRepo = new ResourceAvailabilityRepository() {
        @Override public void save(ResourceAvailability a) {}
        @Override public List<ResourceAvailability> findByResourceId(UUID id) { return List.of(); }
        @Override public List<ResourceAvailability> findByResourceIdAndDate(UUID id, LocalDate date) {
            return List.of(new ResourceAvailability(UUID.randomUUID(),
                    resourceRepo.findById(id).orElse(room),
                    date.atTime(8, 0), date.atTime(18, 0)));
        }
    };

    private final ResourceAssignmentRepository assignmentRepo = new ResourceAssignmentRepository() {
        @Override public void save(ResourceAssignment a) { savedAssignments.add(a); }
        @Override public void deleteByAppointmentAndResource(UUID apptId, UUID resId) {
            deletedAssignments.add(apptId + ":" + resId);
            savedAssignments.removeIf(a -> a.getAppointmentId().equals(apptId)
                    && a.getResourceId().equals(resId));
        }
        @Override public List<ResourceAssignment> findByAppointmentId(UUID id) {
            return savedAssignments.stream()
                    .filter(a -> a.getAppointmentId().equals(id))
                    .toList();
        }
        @Override public List<ResourceAssignment> findByResourceIdAndDate(UUID resId, LocalDate date) {
            return savedAssignments.stream()
                    .filter(a -> a.getResourceId().equals(resId) && a.getDate().equals(date))
                    .toList();
        }
    };

    private final StaffRoomResolver staffRoomResolver = new DefaultStaffRoomResolver();
    private final ResourceRequirementResolver requirementResolver = new DefaultResourceRequirementResolver();

    private ResourceAssignmentService service;

    @BeforeEach
    void setUp() {
        savedAssignments.clear();
        deletedAssignments.clear();
        staffRoomResolver.registerMapping(staffId, roomId);
        service = new ResourceAssignmentService(resourceRepo, availabilityRepo,
                assignmentRepo, requirementResolver, staffRoomResolver);
    }

    @Test
    void autoAssignPicksDefaultRoom() {
        service.autoAssignResources(appointment);

        assertEquals(1, savedAssignments.size());
        assertEquals(roomId, savedAssignments.getFirst().getResourceId());
    }

    @Test
    void autoAssignDoesNotAssignWhenDefaultRoomOccupied() {
        savedAssignments.add(new ResourceAssignment(UUID.randomUUID(), UUID.randomUUID(),
                room, start.toLocalDate()));

        service.autoAssignResources(appointment);

        boolean hasNewAssignment = savedAssignments.stream()
                .anyMatch(a -> a.getAppointmentId().equals(appointmentId));
        assertFalse(hasNewAssignment);
    }

    @Test
    void manualAssignSucceedsWhenAvailable() {
        service.assignResource(appointmentId, vrHeadsetId, start, end);

        assertEquals(1, savedAssignments.size());
        assertEquals(vrHeadsetId, savedAssignments.getFirst().getResourceId());
    }

    @Test
    void manualAssignReplacesExistingOfSameCategory() {
        service.assignResource(appointmentId, roomId, start, end);
        assertEquals(1, savedAssignments.size());

        service.assignResource(appointmentId, overflowRoomId, start, end);

        List<ResourceAssignment> current = assignmentRepo.findByAppointmentId(appointmentId);
        assertEquals(1, current.size());
        assertEquals(overflowRoomId, current.getFirst().getResourceId());
    }

    @Test
    void manualAssignThrowsWhenResourceNotAvailable() {
        ResourceAvailabilityRepository noAvail = new ResourceAvailabilityRepository() {
            @Override public void save(ResourceAvailability a) {}
            @Override public List<ResourceAvailability> findByResourceId(UUID id) { return List.of(); }
            @Override public List<ResourceAvailability> findByResourceIdAndDate(UUID id, LocalDate date) {
                return List.of();
            }
        };

        ResourceAssignmentService svc = new ResourceAssignmentService(resourceRepo, noAvail,
                assignmentRepo, requirementResolver, staffRoomResolver);

        assertThrows(IllegalStateException.class,
                () -> svc.assignResource(appointmentId, roomId, start, end));
    }

    @Test
    void hasRequiredResourcesReturnsFalseWhenMissing() {
        assertFalse(service.hasRequiredResources(appointmentId, AppointmentType.INITIAL_MEDICAL));
    }

    @Test
    void hasRequiredResourcesReturnsTrueWhenSatisfied() {
        service.assignResource(appointmentId, roomId, start, end);
        assertTrue(service.hasRequiredResources(appointmentId, AppointmentType.INITIAL_MEDICAL));
    }

    @Test
    void getAvailableResourcesExcludesBookedOnes() {
        savedAssignments.add(new ResourceAssignment(UUID.randomUUID(), UUID.randomUUID(),
                room, start.toLocalDate()));

        List<Resource> available = service.getAvailableResources(
                ResourceCategory.ROOM, null, start, end);

        assertFalse(available.stream().anyMatch(r -> r.getId().equals(roomId)));
        assertTrue(available.stream().anyMatch(r -> r.getId().equals(overflowRoomId)));
    }

    @Test
    void removeAssignmentDeletesCorrectEntry() {
        service.assignResource(appointmentId, roomId, start, end);
        assertEquals(1, savedAssignments.size());

        service.removeAssignment(appointmentId, roomId);
        assertTrue(deletedAssignments.contains(appointmentId + ":" + roomId));
    }
}
