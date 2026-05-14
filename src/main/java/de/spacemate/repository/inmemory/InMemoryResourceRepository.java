package de.spacemate.repository.inmemory;

import de.spacemate.model.Resource;
import de.spacemate.model.ResourceCategory;
import de.spacemate.repository.ResourceRepository;

import java.util.*;

public class InMemoryResourceRepository implements ResourceRepository {

    private final Map<UUID, Resource> store = new LinkedHashMap<>();

    @Override
    public void save(Resource resource) {
        store.put(resource.getId(), resource);
    }

    @Override
    public Optional<Resource> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Resource> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Resource> findByCategory(ResourceCategory category) {
        return store.values().stream()
                .filter(r -> r.getCategory() == category)
                .toList();
    }

    @Override
    public List<Resource> findByCategoryAndTag(ResourceCategory category, String tag) {
        return store.values().stream()
                .filter(r -> r.getCategory() == category && Objects.equals(r.getTag(), tag))
                .toList();
    }
}
