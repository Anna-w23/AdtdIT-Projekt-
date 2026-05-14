package de.spacemate.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class ResourceAvailability {

    private final UUID id;
    private final Resource resource;
    private final LocalDateTime start;
    private final LocalDateTime end;

    public ResourceAvailability(UUID id, Resource resource, LocalDateTime start, LocalDateTime end) {
        this.id = id;
        this.resource = resource;
        this.start = start;
        this.end = end;
    }

    public UUID getId() { return id; }

    public Resource getResource() { return resource; }

    public UUID getResourceId() { return resource.getId(); }

    public LocalDateTime getStart() { return start; }

    public LocalDateTime getEnd() { return end; }

    public boolean contains(LocalDateTime proposedStart, LocalDateTime proposedEnd) {
        return !proposedStart.isBefore(start) && !proposedEnd.isAfter(end);
    }
}
