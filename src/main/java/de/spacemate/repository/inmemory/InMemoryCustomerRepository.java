package de.spacemate.repository.inmemory;

import de.spacemate.model.Customer;
import de.spacemate.model.OnboardingStage;
import de.spacemate.repository.CustomerRepository;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryCustomerRepository implements CustomerRepository {

    private final Map<UUID, Customer> store = new LinkedHashMap<>();

    @Override
    public void save(Customer customer) {
        store.put(customer.getId(), customer);
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Customer> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Customer> findByStage(OnboardingStage stage) {
        return store.values().stream()
                .filter(c -> c.getCurrentStage() == stage)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(UUID id) {
        store.remove(id);
    }
}
