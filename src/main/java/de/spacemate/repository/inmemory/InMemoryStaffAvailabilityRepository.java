package de.spacemate.repository.inmemory;

import de.spacemate.model.StaffAvailability;
import de.spacemate.repository.StaffAvailabilityRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class InMemoryStaffAvailabilityRepository implements StaffAvailabilityRepository {

    private final Map<UUID, StaffAvailability> store = new LinkedHashMap<>();

    @Override
    public void save(StaffAvailability availability) {
        store.put(availability.getId(), availability);
    }

    @Override
    public List<StaffAvailability> findByStaffId(UUID staffId) {
        return store.values().stream()
                .filter(a -> a.getStaffId().equals(staffId))
                .collect(Collectors.toList());
    }

    @Override
    public List<StaffAvailability> findByStaffIdAndDate(UUID staffId, LocalDate date) {
        return store.values().stream()
                .filter(a -> a.getStaffId().equals(staffId)
                        && a.getStart().toLocalDate().equals(date))
                .collect(Collectors.toList());
    }
}
