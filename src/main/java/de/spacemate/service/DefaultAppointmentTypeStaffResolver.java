package de.spacemate.service;

import de.spacemate.model.*;
import de.spacemate.repository.StaffRepository;

import java.util.List;

public class DefaultAppointmentTypeStaffResolver implements AppointmentTypeStaffResolver {

    private final StaffRepository staffRepository;

    public DefaultAppointmentTypeStaffResolver(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    @Override
    public List<Staff> findQualifiedStaff(AppointmentType type) {
        StaffRole role = type.getRequiredRole();
        if (role == null) {
            return staffRepository.findAll().stream()
                    .filter(s -> s.getRole().isPhysician())
                    .toList();
        }
        return staffRepository.findByRole(role);
    }

    @Override
    public boolean canHandle(AppointmentType type, Staff staff) {
        return findQualifiedStaff(type).contains(staff);
    }
}
