package de.spacemate.factory;

import de.spacemate.model.*;

import java.util.UUID;

public class DefaultStaffFactory implements StaffFactory {

    // OCP: Factory is the designated place for polymorphic instantiation — switch here is intentional
    @Override
    public Staff create(UUID id, String name, StaffRole role) {
        return switch (role) {
            case CHIEF_PHYSICIAN, RESIDENT_PHYSICIAN, NIGHT_PHYSICIAN ->
                new Doctor(id, name, role);
            case EYE_SPECIALIST, CARDIOLOGIST, NEUROLOGIST, ORTHOPEDIST, PSYCHOLOGIST ->
                new Specialist(id, name, role);
            case SPACE_TRAINER, PROCESS_MANAGER ->
                new Trainer(id, name, role);
        };
    }
}
