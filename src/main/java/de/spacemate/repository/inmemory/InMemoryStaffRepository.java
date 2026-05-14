package de.spacemate.repository.inmemory;

import de.spacemate.model.Staff;
import de.spacemate.model.StaffRole;
import de.spacemate.repository.StaffRepository;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryStaffRepository implements StaffRepository {

    private final Map<UUID, Staff> store = new LinkedHashMap<>();

    @Override
    public void save(Staff staff) {
        store.put(staff.getId(), staff);
    }

    @Override
    public Optional<Staff> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Staff> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Staff> findByRole(StaffRole role) {
        return store.values().stream()
                .filter(s -> s.getRole() == role)
                .collect(Collectors.toList());
    }
}
