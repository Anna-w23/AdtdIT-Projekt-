package de.spacemate.repository.inmemory;

import de.spacemate.model.Document;
import de.spacemate.model.DocumentCategory;
import de.spacemate.repository.DocumentRepository;

import java.util.*;

public class InMemoryDocumentRepository implements DocumentRepository {

    private final Map<UUID, Document> store = new LinkedHashMap<>();

    @Override
    public void save(Document document) {
        store.put(document.getId(), document);
    }

    @Override
    public Optional<Document> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Document> findByCustomerId(UUID customerId) {
        return store.values().stream()
                .filter(d -> d.getCustomerId().equals(customerId))
                .toList();
    }

    @Override
    public List<Document> findByCustomerIdAndCategory(UUID customerId, DocumentCategory category) {
        return store.values().stream()
                .filter(d -> d.getCustomerId().equals(customerId) && d.getCategory() == category)
                .toList();
    }
}
