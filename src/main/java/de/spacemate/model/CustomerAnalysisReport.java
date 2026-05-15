package de.spacemate.model;

import java.time.LocalDate;
import java.util.UUID;

public class CustomerAnalysisReport {

    private final UUID id;
    private final Customer customer;
    private final LocalDate generatedOn;
    private boolean specialistConsultationRecommended;
    private boolean lifestyleCoachingRecommended;
    private String summary;

    public CustomerAnalysisReport(UUID id, Customer customer, LocalDate generatedOn) {
        this.id = id;
        this.customer = customer;
        this.generatedOn = generatedOn;
        this.specialistConsultationRecommended = false;
        this.lifestyleCoachingRecommended = false;
    }

    public UUID getId() { return id; }
    public Customer getCustomer() { return customer; }
    public UUID getCustomerId() { return customer.getId(); }
    public LocalDate getGeneratedOn() { return generatedOn; }
    public boolean isSpecialistConsultationRecommended() { return specialistConsultationRecommended; }
    public boolean isLifestyleCoachingRecommended() { return lifestyleCoachingRecommended; }
    public String getSummary() { return summary; }

    public void setSpecialistConsultationRecommended(boolean recommended) { this.specialistConsultationRecommended = recommended; }
    public void setLifestyleCoachingRecommended(boolean recommended) { this.lifestyleCoachingRecommended = recommended; }
    public void setSummary(String summary) { this.summary = summary; }
}
