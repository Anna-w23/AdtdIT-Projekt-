package de.spacemate.model;

import java.util.UUID;

public class Room extends AbstractResource {

    public Room(UUID id, String name, String tag) {
        super(id, name, ResourceCategory.ROOM, tag);
    }
}
