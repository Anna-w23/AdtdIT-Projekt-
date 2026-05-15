# Entity Relationship Diagram

```mermaid
erDiagram

    Customer {
        UUID id PK
        String firstName
        String lastName
        String email
        String preferredLanguage
        boolean indemnityAgreementSigned
        OnboardingStage currentStage
        boolean needsAttention
        String attentionReason
        LocalDate takeoffDate
    }

    Staff {
        UUID id PK
        String name
        StaffRole role
    }

    TimeSlot {
        UUID id PK
        LocalDateTime start
        LocalDateTime end
        boolean booked
    }

    Appointment {
        UUID id PK
        AppointmentType type
        AppointmentStatus status
        LocalDateTime scheduledAt
        String notes
    }

    MedicalReport {
        UUID id PK
        LocalDate issuedOn
        boolean requiresSpecialist
        AppointmentType specialistType
        boolean requiresLifestyleCoaching
        boolean flightEligible
        String remarks
    }

    IndemnityAgreement {
        UUID id PK
        LocalDate sentOn
        boolean signed
        LocalDate signedOn
    }

    CustomerAnalysisReport {
        UUID id PK
        LocalDate generatedOn
        boolean specialistConsultationRecommended
        boolean lifestyleCoachingRecommended
        String summary
    }

    Customer ||--o{ Appointment : "has"
    Customer ||--o{ MedicalReport : "has"
    Customer ||--o| IndemnityAgreement : "signs"
    Customer ||--o{ CustomerAnalysisReport : "has"
    Staff ||--o{ TimeSlot : "has availability"
    Staff ||--o{ Appointment : "conducts"
    TimeSlot ||--o| Appointment : "reserved by"
    Appointment ||--o| MedicalReport : "produces"
```

### Cardinality key
| Notation | Meaning |
|----------|---------|
| `\|\|--o{` | one (mandatory) to zero-or-many |
| `\|\|--o\|` | one (mandatory) to zero-or-one |

### Appointment durations
| Type | Duration |
|------|----------|
| Initial Medical | 60 min |
| Eye Specialist | 45 min |
| Cardiologist | 45 min |
| Neurologist | 45 min |
| Orthopedist | 45 min |
| Psychologist Consultation | 60 min |
| Lifestyle Coaching | 45 min |
| Shuttle Briefing | 90 min |
| Final Medical | 60 min |
