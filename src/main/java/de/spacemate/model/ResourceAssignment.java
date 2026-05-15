package de.spacemate.model;

import java.time.LocalDate;
import java.util.UUID;

public class ResourceAssignment {

    private final UUID id;
    private final UUID appointmentId;
    private final Resource resource;
    private final LocalDate date;

    public ResourceAssignment(UUID id, UUID appointmentId, Resource resource, LocalDate date) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.resource = resource;
        this.date = date;
    }

    public UUID getId() { return id; }

    public UUID getAppointmentId() { return appointmentId; }

    public Resource getResource() { return resource; }

    public UUID getResourceId() { return resource.getId(); }

    public LocalDate getDate() { return date; }
}
