package de.spacemate.repository;

import de.spacemate.model.Customer;
import de.spacemate.model.OnboardingStage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository {
    void save(Customer customer);
    Optional<Customer> findById(UUID id);
    List<Customer> findAll();
    List<Customer> findByStage(OnboardingStage stage);
    void delete(UUID id);
}
