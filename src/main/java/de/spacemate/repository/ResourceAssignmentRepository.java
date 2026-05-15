package de.spacemate.repository;

import de.spacemate.model.ResourceAssignment;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ResourceAssignmentRepository {

    void save(ResourceAssignment assignment);

    void deleteByAppointmentAndResource(UUID appointmentId, UUID resourceId);

    List<ResourceAssignment> findByAppointmentId(UUID appointmentId);

    List<ResourceAssignment> findByResourceIdAndDate(UUID resourceId, LocalDate date);
}
