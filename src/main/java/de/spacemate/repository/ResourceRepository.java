package de.spacemate.repository;

import de.spacemate.model.Resource;
import de.spacemate.model.ResourceCategory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourceRepository {

    void save(Resource resource);

    Optional<Resource> findById(UUID id);

    List<Resource> findByCategory(ResourceCategory category);

    List<Resource> findByCategoryAndTag(ResourceCategory category, String tag);
}
