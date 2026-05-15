package de.spacemate.repository.inmemory;

import de.spacemate.model.DocumentAttachment;
import de.spacemate.repository.DocumentAttachmentRepository;

import java.util.*;

public class InMemoryDocumentAttachmentRepository implements DocumentAttachmentRepository {

    private final Map<UUID, DocumentAttachment> store = new LinkedHashMap<>();

    @Override
    public void save(DocumentAttachment attachment) {
        store.put(attachment.getId(), attachment);
    }

    @Override
    public List<DocumentAttachment> findByAppointmentId(UUID appointmentId) {
        return store.values().stream()
                .filter(a -> a.getAppointmentId().equals(appointmentId))
                .toList();
    }

    @Override
    public void deleteByAppointmentAndDocument(UUID appointmentId, UUID documentId) {
        store.values().removeIf(a ->
                a.getAppointmentId().equals(appointmentId)
                        && a.getDocumentId().equals(documentId));
    }
}
