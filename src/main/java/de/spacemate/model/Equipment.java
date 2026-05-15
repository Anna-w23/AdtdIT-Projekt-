package de.spacemate.model;

import java.util.UUID;

public class Equipment extends AbstractResource {

    public Equipment(UUID id, String name, String tag) {
        super(id, name, ResourceCategory.EQUIPMENT, tag);
    }
}
