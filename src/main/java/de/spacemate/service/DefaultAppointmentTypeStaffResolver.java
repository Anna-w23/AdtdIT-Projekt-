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
        return switch (type) {
            case INITIAL_MEDICAL, FINAL_MEDICAL ->
                staffRepository.findAll().stream()
                        .filter(s -> s instanceof Doctor)
                        .toList();
            case EYE_SPECIALIST    -> staffRepository.findByRole(StaffRole.EYE_SPECIALIST);
            case CARDIOLOGIST      -> staffRepository.findByRole(StaffRole.CARDIOLOGIST);
            case NEUROLOGIST       -> staffRepository.findByRole(StaffRole.NEUROLOGIST);
            case ORTHOPEDIST       -> staffRepository.findByRole(StaffRole.ORTHOPEDIST);
            case PSYCHOLOGIST_CONSULTATION -> staffRepository.findByRole(StaffRole.PSYCHOLOGIST);
            case SPACE_TRAINING    -> staffRepository.findByRole(StaffRole.SPACE_TRAINER);
        };
    }
}
