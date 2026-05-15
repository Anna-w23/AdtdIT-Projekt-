package de.spacemate.factory;

import de.spacemate.model.Resource;
import de.spacemate.model.ResourceCategory;

import java.util.UUID;

public interface ResourceFactory {

    Resource create(UUID id, String name, ResourceCategory category, String tag);
}
