package de.spacemate.model;

import java.time.LocalDate;
import java.util.UUID;

public class Customer {

    private final UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String preferredLanguage;
    private boolean indemnityAgreementSigned;
    private OnboardingStage currentStage;
    private boolean needsAttention;
    private String attentionReason;
    private LocalDate takeoffDate;

    public Customer(UUID id, String firstName, String lastName, String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.preferredLanguage = "EN";
        this.indemnityAgreementSigned = false;
        this.currentStage = OnboardingStage.REGISTERED;
    }

    public UUID getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return firstName + " " + lastName; }
    public String getEmail() { return email; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public boolean isIndemnityAgreementSigned() { return indemnityAgreementSigned; }
    public OnboardingStage getCurrentStage() { return currentStage; }

    public boolean isNeedsAttention() { return needsAttention; }
    public String getAttentionReason() { return attentionReason; }
    public LocalDate getTakeoffDate() { return takeoffDate; }

    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setEmail(String email) { this.email = email; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }
    public void setIndemnityAgreementSigned(boolean signed) { this.indemnityAgreementSigned = signed; }
    public void setCurrentStage(OnboardingStage stage) { this.currentStage = stage; }
    public void setNeedsAttention(boolean needsAttention, String reason) {
        this.needsAttention = needsAttention;
        this.attentionReason = needsAttention ? reason : null;
    }
    public void setTakeoffDate(LocalDate takeoffDate) { this.takeoffDate = takeoffDate; }

    @Override
    public String toString() {
        return getFullName() + " [" + currentStage + "]";
    }
}
