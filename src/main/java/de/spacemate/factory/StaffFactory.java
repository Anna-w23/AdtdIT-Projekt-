package de.spacemate.factory;

import de.spacemate.model.Staff;
import de.spacemate.model.StaffRole;

import java.util.UUID;

public interface StaffFactory {
    Staff create(UUID id, String name, StaffRole role);
}
