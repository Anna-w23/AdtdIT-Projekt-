package de.spacemate.repository;

import de.spacemate.model.ResourceAvailability;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ResourceAvailabilityRepository {

    void save(ResourceAvailability availability);

    List<ResourceAvailability> findByResourceId(UUID resourceId);

    List<ResourceAvailability> findByResourceIdAndDate(UUID resourceId, LocalDate date);
}
