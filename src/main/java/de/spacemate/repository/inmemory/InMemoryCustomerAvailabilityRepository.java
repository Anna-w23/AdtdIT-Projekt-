package de.spacemate.repository.inmemory;

import de.spacemate.repository.CustomerAvailabilityRepository;

import java.time.LocalDateTime;
import java.util.*;

public class InMemoryCustomerAvailabilityRepository implements CustomerAvailabilityRepository {

    private final Map<UUID, Set<LocalDateTime>> store = new HashMap<>();

    @Override
    public void setAvailability(UUID customerId, Set<LocalDateTime> slots) {
        store.put(customerId, new LinkedHashSet<>(slots));
    }

    @Override
    public Set<LocalDateTime> getAvailability(UUID customerId) {
        return store.getOrDefault(customerId, Collections.emptySet());
    }

    @Override
    public void removeSlot(UUID customerId, LocalDateTime slot) {
        Set<LocalDateTime> slots = store.get(customerId);
        if (slots != null) {
            slots.remove(slot);
        }
    }
}
