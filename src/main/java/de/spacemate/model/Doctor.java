package de.spacemate.model;

import java.util.UUID;

public class Doctor extends AbstractStaff {

    public Doctor(UUID id, String name, StaffRole role) {
        super(id, name, role);
    }
}
