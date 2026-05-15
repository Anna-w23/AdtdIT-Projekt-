package de.spacemate.repository.inmemory;

import de.spacemate.model.TimeSlot;
import de.spacemate.repository.TimeSlotRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class InMemoryTimeSlotRepository implements TimeSlotRepository {

    private final Map<UUID, TimeSlot> store = new LinkedHashMap<>();

    @Override
    public void save(TimeSlot slot) {
        store.put(slot.getId(), slot);
    }

    @Override
    public Optional<TimeSlot> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<TimeSlot> findAvailableByStaffId(UUID staffId) {
        return store.values().stream()
                .filter(s -> s.getStaffId().equals(staffId) && !s.isBooked())
                .collect(Collectors.toList());
    }

    @Override
    public List<TimeSlot> findBookedByStaffIdAndDate(UUID staffId, LocalDate date) {
        return store.values().stream()
                .filter(s -> s.getStaffId().equals(staffId) && s.isBooked()
                        && s.getStart().toLocalDate().equals(date))
                .collect(Collectors.toList());
    }

    @Override
    public void markBooked(UUID slotId) {
        TimeSlot slot = store.get(slotId);
        if (slot == null) throw new NoSuchElementException("TimeSlot not found: " + slotId);
        if (slot.isBooked()) throw new IllegalStateException("TimeSlot already booked: " + slotId);
        slot.setBooked(true);
    }

    @Override
    public void markFree(UUID slotId) {
        TimeSlot slot = store.get(slotId);
        if (slot == null) throw new NoSuchElementException("TimeSlot not found: " + slotId);
        slot.setBooked(false);
    }
}
