package de.spacemate.factory;

import de.spacemate.model.Customer;

import java.util.UUID;

public class DefaultCustomerFactory implements CustomerFactory {

    @Override
    public Customer create(UUID id, String firstName, String lastName, String email) {
        return new Customer(id, firstName, lastName, email);
    }
}
