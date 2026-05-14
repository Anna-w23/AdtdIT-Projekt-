package de.spacemate.repository;

import de.spacemate.model.Document;
import de.spacemate.model.DocumentCategory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository {
    void save(Document document);
    Optional<Document> findById(UUID id);
    List<Document> findByCustomerId(UUID customerId);
    List<Document> findByCustomerIdAndCategory(UUID customerId, DocumentCategory category);
}
