package de.spacemate.factory;

import de.spacemate.model.Customer;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DefaultCustomerFactoryTest {

    private final CustomerFactory factory = new DefaultCustomerFactory();

    @Test
    void createsCustomerWithCorrectFields() {
        UUID id = UUID.randomUUID();
        Customer customer = factory.create(id, "Maria", "Santos", "maria@test.com");

        assertEquals(id, customer.getId());
        assertEquals("Maria", customer.getFirstName());
        assertEquals("Santos", customer.getLastName());
        assertEquals("maria@test.com", customer.getEmail());
    }

    @Test
    void newCustomerStartsInRegisteredStage() {
        Customer customer = factory.create(UUID.randomUUID(), "Test", "User", "t@t.com");
        assertEquals(de.spacemate.model.OnboardingStage.REGISTERED, customer.getCurrentStage());
    }

    @Test
    void newCustomerHasEnglishAsDefaultLanguage() {
        Customer customer = factory.create(UUID.randomUUID(), "Test", "User", "t@t.com");
        assertEquals("EN", customer.getPreferredLanguage());
    }
}
