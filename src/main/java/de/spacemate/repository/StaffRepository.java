package de.spacemate.repository;

import de.spacemate.model.Staff;
import de.spacemate.model.StaffRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffRepository {
    void save(Staff staff);
    Optional<Staff> findById(UUID id);
    List<Staff> findAll();
    List<Staff> findByRole(StaffRole role);
}
