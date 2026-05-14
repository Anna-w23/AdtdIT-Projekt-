package de.spacemate.model;

import java.util.UUID;

public interface Resource {

    UUID getId();

    String getName();

    ResourceCategory getCategory();

    String getTag();
}
