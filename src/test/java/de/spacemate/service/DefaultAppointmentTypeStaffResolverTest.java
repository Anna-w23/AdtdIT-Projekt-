package de.spacemate.service;

import de.spacemate.model.*;
import de.spacemate.repository.StaffRepository;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DefaultAppointmentTypeStaffResolverTest {

    private final Doctor doctor = new Doctor(UUID.randomUUID(), "Dr. Test", StaffRole.CHIEF_PHYSICIAN);
    private final Specialist eyeSpecialist = new Specialist(UUID.randomUUID(), "Dr. Eye", StaffRole.EYE_SPECIALIST);
    private final Trainer spaceTrainer = new Trainer(UUID.randomUUID(), "Tom", StaffRole.SPACE_TRAINER);

    private final StaffRepository staffRepo = new StaffRepository() {
        private final List<Staff> all = List.of(doctor, eyeSpecialist, spaceTrainer);

        @Override public void save(Staff staff) {}
        @Override public Optional<Staff> findById(UUID id) { return all.stream().filter(s -> s.getId().equals(id)).findFirst(); }
        @Override public List<Staff> findAll() { return all; }
        @Override public List<Staff> findByRole(StaffRole role) { return all.stream().filter(s -> s.getRole() == role).toList(); }
    };

    private final AppointmentTypeStaffResolver resolver = new DefaultAppointmentTypeStaffResolver(staffRepo);

    @Test
    void initialMedicalReturnsDoctorsOnly() {
        List<Staff> result = resolver.findQualifiedStaff(AppointmentType.INITIAL_MEDICAL);
        assertEquals(1, result.size());
        assertInstanceOf(Doctor.class, result.getFirst());
    }

    @Test
    void finalMedicalReturnsDoctorsOnly() {
        List<Staff> result = resolver.findQualifiedStaff(AppointmentType.FINAL_MEDICAL);
        assertEquals(1, result.size());
        assertInstanceOf(Doctor.class, result.getFirst());
    }

    @Test
    void eyeSpecialistReturnsMatchingRole() {
        List<Staff> result = resolver.findQualifiedStaff(AppointmentType.EYE_SPECIALIST);
        assertEquals(1, result.size());
        assertEquals(StaffRole.EYE_SPECIALIST, result.getFirst().getRole());
    }

    @Test
    void spaceTrainingReturnsSpaceTrainer() {
        List<Staff> result = resolver.findQualifiedStaff(AppointmentType.SPACE_TRAINING);
        assertEquals(1, result.size());
        assertEquals(StaffRole.SPACE_TRAINER, result.getFirst().getRole());
    }

    @Test
    void unknownSpecialistReturnsEmptyList() {
        List<Staff> result = resolver.findQualifiedStaff(AppointmentType.CARDIOLOGIST);
        assertTrue(result.isEmpty());
    }
}
