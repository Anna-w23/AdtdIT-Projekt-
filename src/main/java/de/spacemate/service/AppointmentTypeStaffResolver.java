package de.spacemate.service;

import de.spacemate.model.AppointmentType;
import de.spacemate.model.Staff;

import java.util.List;

public interface AppointmentTypeStaffResolver {
    List<Staff> findQualifiedStaff(AppointmentType type);
}
