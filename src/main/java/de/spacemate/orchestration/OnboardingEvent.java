package de.spacemate.orchestration;

import de.spacemate.model.Customer;

/**
 * Immutable snapshot fired to all registered listeners after a state transition.
 * The payload is the affected Customer plus a human-readable message.
 */
public class OnboardingEvent {

    private final OnboardingEventType type;
    private final Customer customer;
    private final String message;

    public OnboardingEvent(OnboardingEventType type, Customer customer, String message) {
        this.type = type;
        this.customer = customer;
        this.message = message;
    }

    public OnboardingEventType getType() { return type; }
    public Customer getCustomer() { return customer; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        return "[" + type + "] " + customer.getFullName() + " – " + message;
    }
}
