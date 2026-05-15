package de.spacemate.repository;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public interface CustomerAvailabilityRepository {
    void setAvailability(UUID customerId, Set<LocalDateTime> slots);
    Set<LocalDateTime> getAvailability(UUID customerId);
    void removeSlot(UUID customerId, LocalDateTime slot);
}
