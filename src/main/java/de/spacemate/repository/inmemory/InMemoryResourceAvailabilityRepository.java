package de.spacemate.repository.inmemory;

import de.spacemate.model.ResourceAvailability;
import de.spacemate.repository.ResourceAvailabilityRepository;

import java.time.LocalDate;
import java.util.*;

public class InMemoryResourceAvailabilityRepository implements ResourceAvailabilityRepository {

    private final Map<UUID, ResourceAvailability> store = new LinkedHashMap<>();

    @Override
    public void save(ResourceAvailability availability) {
        store.put(availability.getId(), availability);
    }

    @Override
    public List<ResourceAvailability> findByResourceId(UUID resourceId) {
        return store.values().stream()
                .filter(a -> a.getResourceId().equals(resourceId))
                .toList();
    }

    @Override
    public List<ResourceAvailability> findByResourceIdAndDate(UUID resourceId, LocalDate date) {
        return store.values().stream()
                .filter(a -> a.getResourceId().equals(resourceId)
                        && a.getStart().toLocalDate().equals(date))
                .toList();
    }
}
