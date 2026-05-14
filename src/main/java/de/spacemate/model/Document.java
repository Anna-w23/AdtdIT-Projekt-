package de.spacemate.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Document {

    private final UUID id;
    private final UUID customerId;
    private final DocumentType type;
    private final DocumentCategory category;
    private final DocumentDirection direction;
    private final LocalDateTime createdAt;
    private final String content;
    private final Map<String, String> metadata;

    public Document(UUID id, UUID customerId, DocumentType type, DocumentCategory category,
                    DocumentDirection direction, LocalDateTime createdAt, String content) {
        this.id = id;
        this.customerId = customerId;
        this.type = type;
        this.category = category;
        this.direction = direction;
        this.createdAt = createdAt;
        this.content = content;
        this.metadata = new HashMap<>();
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public DocumentType getType() { return type; }
    public DocumentCategory getCategory() { return category; }
    public DocumentDirection getDirection() { return direction; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getContent() { return content; }
    public Map<String, String> getMetadata() { return metadata; }

    public void putMetadata(String key, String value) {
        metadata.put(key, value);
    }

    public String getMetadataValue(String key) {
        return metadata.get(key);
    }
}
