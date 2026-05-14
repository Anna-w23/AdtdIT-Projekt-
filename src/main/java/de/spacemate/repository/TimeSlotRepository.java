package de.spacemate.repository;

import de.spacemate.model.TimeSlot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeSlotRepository {
    void save(TimeSlot slot);
    Optional<TimeSlot> findById(UUID id);
    List<TimeSlot> findAvailableByStaffId(UUID staffId);
    List<TimeSlot> findBookedByStaffIdAndDate(UUID staffId, LocalDate date);
    void markBooked(UUID slotId);
    void markFree(UUID slotId);
}
