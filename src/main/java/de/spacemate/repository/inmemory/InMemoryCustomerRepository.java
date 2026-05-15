package de.spacemate.repository.inmemory;

import de.spacemate.model.Customer;
import de.spacemate.repository.CustomerRepository;

import java.util.*;

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
}
