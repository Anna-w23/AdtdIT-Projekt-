package de.spacemate.repository;

import de.spacemate.model.DocumentAttachment;

import java.util.List;
import java.util.UUID;

public interface DocumentAttachmentRepository {
    void save(DocumentAttachment attachment);
    List<DocumentAttachment> findByAppointmentId(UUID appointmentId);
    void deleteByAppointmentAndDocument(UUID appointmentId, UUID documentId);
}
