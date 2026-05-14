package de.spacemate.factory;

import de.spacemate.model.*;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DefaultStaffFactoryTest {

    private final StaffFactory factory = new DefaultStaffFactory();

    @Test
    void createsDoctorForChiefPhysician() {
        Staff staff = factory.create(UUID.randomUUID(), "Dr. Test", StaffRole.CHIEF_PHYSICIAN);
        assertInstanceOf(Doctor.class, staff);
    }

    @Test
    void createsDoctorForResidentPhysician() {
        Staff staff = factory.create(UUID.randomUUID(), "Dr. Test", StaffRole.RESIDENT_PHYSICIAN);
        assertInstanceOf(Doctor.class, staff);
    }

    @Test
    void createsDoctorForNightPhysician() {
        Staff staff = factory.create(UUID.randomUUID(), "Dr. Test", StaffRole.NIGHT_PHYSICIAN);
        assertInstanceOf(Doctor.class, staff);
    }

    @Test
    void createsSpecialistForEyeSpecialist() {
        Staff staff = factory.create(UUID.randomUUID(), "Dr. Eye", StaffRole.EYE_SPECIALIST);
        assertInstanceOf(Specialist.class, staff);
    }

    @Test
    void createsSpecialistForCardiologist() {
        Staff staff = factory.create(UUID.randomUUID(), "Dr. Heart", StaffRole.CARDIOLOGIST);
        assertInstanceOf(Specialist.class, staff);
    }

    @Test
    void createsTrainerForSpaceTrainer() {
        Staff staff = factory.create(UUID.randomUUID(), "Tom", StaffRole.SPACE_TRAINER);
        assertInstanceOf(Trainer.class, staff);
    }

    @Test
    void createsTrainerForProcessManager() {
        Staff staff = factory.create(UUID.randomUUID(), "Max", StaffRole.PROCESS_MANAGER);
        assertInstanceOf(Trainer.class, staff);
    }

    @Test
    void assignsIdAndNameCorrectly() {
        UUID id = UUID.randomUUID();
        Staff staff = factory.create(id, "Dr. Elena Richter", StaffRole.CHIEF_PHYSICIAN);
        assertEquals(id, staff.getId());
        assertEquals("Dr. Elena Richter", staff.getName());
        assertEquals(StaffRole.CHIEF_PHYSICIAN, staff.getRole());
    }
}
