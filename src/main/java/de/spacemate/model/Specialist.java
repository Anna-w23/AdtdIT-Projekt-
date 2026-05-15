package de.spacemate.model;

import java.util.UUID;

public class Specialist extends AbstractStaff {

    public Specialist(UUID id, String name, StaffRole role) {
        super(id, name, role);
    }
}
