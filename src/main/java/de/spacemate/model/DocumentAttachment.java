package de.spacemate.model;

import java.util.UUID;

public class DocumentAttachment {

    private final UUID id;
    private final UUID appointmentId;
    private final UUID documentId;

    public DocumentAttachment(UUID id, UUID appointmentId, UUID documentId) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.documentId = documentId;
    }

    public UUID getId() { return id; }
    public UUID getAppointmentId() { return appointmentId; }
    public UUID getDocumentId() { return documentId; }
}
