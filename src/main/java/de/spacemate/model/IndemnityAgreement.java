package de.spacemate.model;

import java.time.LocalDate;
import java.util.UUID;

public class IndemnityAgreement {

    private final UUID id;
    private final Customer customer;
    private final LocalDate sentOn;
    private boolean signed;
    private LocalDate signedOn;

    public IndemnityAgreement(UUID id, Customer customer, LocalDate sentOn) {
        this.id = id;
        this.customer = customer;
        this.sentOn = sentOn;
        this.signed = false;
    }

    public UUID getId() { return id; }
    public Customer getCustomer() { return customer; }
    public UUID getCustomerId() { return customer.getId(); }
    public LocalDate getSentOn() { return sentOn; }
    public boolean isSigned() { return signed; }
    public LocalDate getSignedOn() { return signedOn; }

    public void markSigned(LocalDate signedOn) {
        this.signed = true;
        this.signedOn = signedOn;
    }
}
