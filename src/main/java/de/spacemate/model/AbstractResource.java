package de.spacemate.model;

import java.util.UUID;

public abstract class AbstractResource implements Resource {

    private final UUID id;
    private final String name;
    private final ResourceCategory category;
    private final String tag;

    protected AbstractResource(UUID id, String name, ResourceCategory category, String tag) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.tag = tag;
    }

    @Override
    public UUID getId() { return id; }

    @Override
    public String getName() { return name; }

    @Override
    public ResourceCategory getCategory() { return category; }

    @Override
    public String getTag() { return tag; }

    @Override
    public String toString() {
        return name + " (" + tag + ")";
    }
}
