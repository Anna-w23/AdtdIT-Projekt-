package de.spacemate.repository;

import de.spacemate.model.StaffAvailability;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface StaffAvailabilityRepository {
    void save(StaffAvailability availability);
    List<StaffAvailability> findByStaffId(UUID staffId);
    List<StaffAvailability> findByStaffIdAndDate(UUID staffId, LocalDate date);
}
