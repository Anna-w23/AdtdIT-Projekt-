package de.spacemate.factory;

import de.spacemate.model.Customer;

import java.util.UUID;

public interface CustomerFactory {
    Customer create(UUID id, String firstName, String lastName, String email);
}
