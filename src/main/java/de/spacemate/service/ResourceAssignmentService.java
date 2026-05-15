package de.spacemate.service;

import de.spacemate.model.*;
import de.spacemate.repository.ResourceAssignmentRepository;
import de.spacemate.repository.ResourceAvailabilityRepository;
import de.spacemate.repository.ResourceRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ResourceAssignmentService {

    private final ResourceRepository resourceRepository;
    private final ResourceAvailabilityRepository availabilityRepository;
    private final ResourceAssignmentRepository assignmentRepository;
    private final ResourceRequirementResolver requirementResolver;
    private final StaffRoomResolver staffRoomResolver;

    public ResourceAssignmentService(ResourceRepository resourceRepository,
                                     ResourceAvailabilityRepository availabilityRepository,
                                     ResourceAssignmentRepository assignmentRepository,
                                     ResourceRequirementResolver requirementResolver,
                                     StaffRoomResolver staffRoomResolver) {
        this.resourceRepository = resourceRepository;
        this.availabilityRepository = availabilityRepository;
        this.assignmentRepository = assignmentRepository;
        this.requirementResolver = requirementResolver;
        this.staffRoomResolver = staffRoomResolver;
    }

    public void autoAssignResources(Appointment appointment) {
        List<ResourceRequirement> requirements = requirementResolver.getRequirements(appointment.getType());
        LocalDateTime start = appointment.getTimeSlot().getStart();
        LocalDateTime end = appointment.getTimeSlot().getEnd();

        for (ResourceRequirement req : requirements) {
            if (!req.autoAssign()) continue;

            UUID resourceId = null;

            if (req.category() == ResourceCategory.ROOM) {
                Optional<UUID> defaultRoom = staffRoomResolver.getDefaultRoom(appointment.getStaffId());
                if (defaultRoom.isPresent() && isAvailable(defaultRoom.get(), start, end)) {
                    resourceId = defaultRoom.get();
                }
            }

            if (resourceId != null) {
                doAssign(appointment.getId(), resourceId, start);
            }
        }
    }

    public void assignResource(UUID appointmentId, UUID resourceId, LocalDateTime start, LocalDateTime end) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + resourceId));

        if (!isAvailable(resourceId, start, end)) {
            throw new IllegalStateException("Resource " + resource.getName() + " is not available for the requested time");
        }

        List<ResourceAssignment> existing = assignmentRepository.findByAppointmentId(appointmentId);
        existing.stream()
                .filter(a -> shouldReplace(a, resource))
                .forEach(a -> assignmentRepository.deleteByAppointmentAndResource(appointmentId, a.getResourceId()));

        doAssign(appointmentId, resourceId, start);
    }

    public void removeAssignment(UUID appointmentId, UUID resourceId) {
        assignmentRepository.deleteByAppointmentAndResource(appointmentId, resourceId);
    }

    public void removeAllAssignments(UUID appointmentId) {
        List<ResourceAssignment> assignments = assignmentRepository.findByAppointmentId(appointmentId);
        for (ResourceAssignment a : assignments) {
            assignmentRepository.deleteByAppointmentAndResource(appointmentId, a.getResourceId());
        }
    }

    public List<ResourceAssignment> getAssignments(UUID appointmentId) {
        return assignmentRepository.findByAppointmentId(appointmentId);
    }

    public List<Resource> getAvailableResources(ResourceCategory category, String tag,
                                                 LocalDateTime start, LocalDateTime end) {
        return getAvailableResources(category, tag, start, end, null);
    }

    public List<Resource> getAvailableResources(ResourceCategory category, String tag,
                                                 LocalDateTime start, LocalDateTime end,
                                                 UUID excludeAppointmentId) {
        List<Resource> candidates = tag != null
                ? resourceRepository.findByCategoryAndTag(category, tag)
                : resourceRepository.findByCategory(category);

        return candidates.stream()
                .filter(r -> excludeAppointmentId != null
                        ? isAvailableExcluding(r.getId(), start, end, excludeAppointmentId)
                        : isAvailable(r.getId(), start, end))
                .toList();
    }

    public boolean hasRequiredResources(UUID appointmentId, AppointmentType type) {
        return allRequirementsSatisfied(appointmentId, requirementResolver.getRequirements(type));
    }

    public boolean hasRequiredResources(UUID appointmentId, Appointment appointment) {
        return allRequirementsSatisfied(appointmentId, requirementResolver.getRequirements(appointment));
    }

    private boolean allRequirementsSatisfied(UUID appointmentId, List<ResourceRequirement> requirements) {
        List<ResourceAssignment> assignments = assignmentRepository.findByAppointmentId(appointmentId);
        for (ResourceRequirement req : requirements) {
            boolean satisfied = assignments.stream().anyMatch(a ->
                    a.getResource().getCategory() == req.category()
                            && (req.tag() == null || req.tag().equals(a.getResource().getTag())));
            if (!satisfied) return false;
        }
        return true;
    }

    public List<ResourceRequirement> getResourceRequirements(Appointment appointment) {
        return requirementResolver.getRequirements(appointment);
    }

    public List<ResourceAvailability> getResourceAvailability(UUID resourceId) {
        return availabilityRepository.findByResourceId(resourceId);
    }

    public List<ResourceRequirement> getResourceRequirements(AppointmentType type) {
        return requirementResolver.getRequirements(type);
    }

    public List<ResourceAssignment> getBookedResourceSlots(UUID resourceId, java.time.LocalDate date) {
        return assignmentRepository.findByResourceIdAndDate(resourceId, date);
    }

    private boolean isAvailable(UUID resourceId, LocalDateTime start, LocalDateTime end) {
        return isAvailableExcluding(resourceId, start, end, null);
    }

    private boolean isAvailableExcluding(UUID resourceId, LocalDateTime start, LocalDateTime end,
                                          UUID excludeAppointmentId) {
        List<ResourceAvailability> windows = availabilityRepository
                .findByResourceIdAndDate(resourceId, start.toLocalDate());
        boolean withinWindow = windows.stream().anyMatch(w -> w.contains(start, end));
        if (!withinWindow) return false;

        List<ResourceAssignment> bookings = assignmentRepository
                .findByResourceIdAndDate(resourceId, start.toLocalDate());
        if (excludeAppointmentId == null) return bookings.isEmpty();
        return bookings.stream().allMatch(b -> b.getAppointmentId().equals(excludeAppointmentId));
    }

    private boolean shouldReplace(ResourceAssignment existing, Resource newResource) {
        if (newResource.getCategory() == ResourceCategory.ROOM) {
            return existing.getResource().getCategory() == ResourceCategory.ROOM;
        }
        return existing.getResource().getCategory() == newResource.getCategory()
                && java.util.Objects.equals(existing.getResource().getTag(), newResource.getTag());
    }

    private void doAssign(UUID appointmentId, UUID resourceId, LocalDateTime appointmentStart) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + resourceId));
        ResourceAssignment assignment = new ResourceAssignment(
                UUID.randomUUID(), appointmentId, resource, appointmentStart.toLocalDate());
        assignmentRepository.save(assignment);
    }
}
