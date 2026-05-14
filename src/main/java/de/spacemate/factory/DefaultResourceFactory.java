package de.spacemate.factory;

import de.spacemate.model.*;

import java.util.UUID;

public class DefaultResourceFactory implements ResourceFactory {

    @Override
    public Resource create(UUID id, String name, ResourceCategory category, String tag) {
        return switch (category) {
            case ROOM -> new Room(id, name, tag);
            case EQUIPMENT -> new Equipment(id, name, tag);
        };
    }
}
