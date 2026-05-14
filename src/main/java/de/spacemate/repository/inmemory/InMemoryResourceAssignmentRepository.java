package de.spacemate.repository.inmemory;

import de.spacemate.model.ResourceAssignment;
import de.spacemate.repository.ResourceAssignmentRepository;

import java.time.LocalDate;
import java.util.*;

public class InMemoryResourceAssignmentRepository implements ResourceAssignmentRepository {

    private final Map<UUID, ResourceAssignment> store = new LinkedHashMap<>();

    @Override
    public void save(ResourceAssignment assignment) {
        store.put(assignment.getId(), assignment);
    }

    @Override
    public void deleteByAppointmentAndResource(UUID appointmentId, UUID resourceId) {
        store.values().removeIf(a ->
                a.getAppointmentId().equals(appointmentId)
                        && a.getResourceId().equals(resourceId));
    }

    @Override
    public List<ResourceAssignment> findByAppointmentId(UUID appointmentId) {
        return store.values().stream()
                .filter(a -> a.getAppointmentId().equals(appointmentId))
                .toList();
    }

    @Override
    public List<ResourceAssignment> findByResourceIdAndDate(UUID resourceId, LocalDate date) {
        return store.values().stream()
                .filter(a -> a.getResourceId().equals(resourceId) && a.getDate().equals(date))
                .toList();
    }
}
