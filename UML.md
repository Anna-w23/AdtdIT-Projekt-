# UML Class Diagram — SpaceMate

The full architecture is split into five focused diagrams, one per layer.
Paste each code block separately into [mermaid.live](https://mermaid.live).

---

## Notation

| Symbol | Meaning |
|--------|---------|
| `◄──` | Dependency: class uses another class |
| `◄..` | Realisation: class implements an interface |
| `◄\|──` | Inheritance: class extends another class |
| `o--` | Aggregation: container holds reference |
| `-` | private visibility |

---

## Diagram 1 — Data Model (`model` package)

> Pure data classes and enums. Staff is an interface implemented by Doctor, Specialist, and Trainer.
> AppointmentType carries duration metadata for scheduling validation.

```mermaid
classDiagram
    direction TB

    class Staff {
        <<interface>>
        +UUID getId()
        +String getName()
        +StaffRole getRole()
        +List~TimeSlot~ getAvailability()
        +void addTimeSlot(TimeSlot)
    }

    class AbstractStaff {
        <<abstract>>
        -UUID id
        -String name
        -StaffRole role
        -List~TimeSlot~ availability
    }

    class Doctor {
    }

    class Specialist {
    }

    class Trainer {
    }

    class Customer {
        -UUID id
        -String firstName
        -String lastName
        -String email
        -String preferredLanguage
        -boolean indemnityAgreementSigned
        -OnboardingStage currentStage
        -boolean needsAttention
        -String attentionReason
        -LocalDate takeoffDate
        +String getFullName()
        +void setCurrentStage(OnboardingStage)
        +void setNeedsAttention(boolean, String)
    }

    class Appointment {
        -UUID id
        -Customer customer
        -Staff conductor
        -TimeSlot timeSlot
        -AppointmentType type
        -AppointmentStatus status
        -LocalDateTime scheduledAt
        +void setStatus(AppointmentStatus)
    }

    class MedicalReport {
        -UUID id
        -Customer customer
        -Appointment appointment
        -LocalDate issuedOn
        -boolean requiresSpecialist
        -AppointmentType specialistType
        -boolean requiresLifestyleCoaching
        -boolean flightEligible
        -String remarks
    }

    class IndemnityAgreement {
        -UUID id
        -Customer customer
        -LocalDate sentOn
        -boolean signed
        -LocalDate signedOn
        +void markSigned(LocalDate)
    }

    class TimeSlot {
        -UUID id
        -Staff staff
        -LocalDateTime start
        -LocalDateTime end
        -boolean booked
    }

    class CustomerAnalysisReport {
        -UUID id
        -Customer customer
        -LocalDate generatedOn
        -boolean specialistConsultationRecommended
        -boolean lifestyleCoachingRecommended
        -String summary
    }

    class OnboardingStage {
        <<enumeration>>
        REGISTERED
        QUESTIONNAIRE_SENT
        QUESTIONNAIRE_COMPLETED
        INDEMNITY_PENDING
        INDEMNITY_SIGNED
        FIRST_MEDICAL_SCHEDULED
        FIRST_MEDICAL_COMPLETED
        SPECIALIST_SCHEDULED
        SPECIALIST_COMPLETED
        LIFESTYLE_COACHING_SCHEDULED
        LIFESTYLE_COACHING_COMPLETED
        SHUTTLE_BRIEFING_SCHEDULED
        SHUTTLE_BRIEFING_COMPLETED
        FINAL_MEDICAL_SCHEDULED
        FINAL_MEDICAL_COMPLETED
        APPROVED
        REJECTED
        APPOINTMENT_REFUSED
    }

    class AppointmentType {
        <<enumeration>>
        INITIAL_MEDICAL (60 min)
        EYE_SPECIALIST (45 min)
        CARDIOLOGIST (45 min)
        NEUROLOGIST (45 min)
        ORTHOPEDIST (45 min)
        PSYCHOLOGIST_CONSULTATION (60 min)
        LIFESTYLE_COACHING (45 min)
        SHUTTLE_BRIEFING (90 min)
        FINAL_MEDICAL (60 min)
        -Duration duration
        +Duration getDuration()
        +String displayName()
        +boolean isSpecialist()
        +OnboardingStage scheduledStage()
    }

    class AppointmentStatus {
        <<enumeration>>
        SUGGESTED
        CONFIRMED
        COMPLETED
        CANCELLED
    }

    class StaffRole {
        <<enumeration>>
        CHIEF_PHYSICIAN
        RESIDENT_PHYSICIAN
        NIGHT_PHYSICIAN
        EYE_SPECIALIST
        CARDIOLOGIST
        NEUROLOGIST
        ORTHOPEDIST
        PSYCHOLOGIST
        LIFESTYLE_COACH
        SHUTTLE_TRAINER
        PROCESS_MANAGER
    }

    AbstractStaff ..|> Staff : implements
    Doctor --|> AbstractStaff : extends
    Specialist --|> AbstractStaff : extends
    Trainer --|> AbstractStaff : extends

    Customer --> OnboardingStage : current pipeline stage
    Appointment --> AppointmentType : what kind
    Appointment --> AppointmentStatus : current status
    Appointment --> Customer : belongs to
    Appointment --> Staff : conducted by
    Appointment --> TimeSlot : reserves
    MedicalReport --> Customer : written for
    MedicalReport --> Appointment : result of
    MedicalReport --> AppointmentType : specialist referral type
    IndemnityAgreement --> Customer : signed by
    CustomerAnalysisReport --> Customer : generated for
    Staff --> StaffRole : has role
    TimeSlot --> Staff : assigned to
```

---

## Diagram 2 — Repository Layer (`repository` package)

> Interfaces define the storage contract. InMemory implementations fulfil it.
> Swapping to a database only requires new implementations — no service changes.

```mermaid
classDiagram
    direction LR

    class CustomerRepository {
        <<interface>>
        +void save(Customer)
        +Optional~Customer~ findById(UUID)
        +List~Customer~ findAll()
        +List~Customer~ findByStage(OnboardingStage)
        +void delete(UUID)
    }
    class InMemoryCustomerRepository

    class AppointmentRepository {
        <<interface>>
        +void save(Appointment)
        +Optional~Appointment~ findById(UUID)
        +List~Appointment~ findByCustomerId(UUID)
        +List~Appointment~ findByCustomerIdAndType(UUID, AppointmentType)
        +List~Appointment~ findByDateAndStatus(LocalDate, AppointmentStatus)
    }
    class InMemoryAppointmentRepository

    class StaffRepository {
        <<interface>>
        +void save(Staff)
        +Optional~Staff~ findById(UUID)
        +List~Staff~ findAll()
        +List~Staff~ findByRole(StaffRole)
    }
    class InMemoryStaffRepository

    class TimeSlotRepository {
        <<interface>>
        +void save(TimeSlot)
        +Optional~TimeSlot~ findById(UUID)
        +List~TimeSlot~ findAvailableByStaffId(UUID)
        +void markBooked(UUID)
        +void markFree(UUID)
    }
    class InMemoryTimeSlotRepository

    class MedicalReportRepository {
        <<interface>>
        +void save(MedicalReport)
        +Optional~MedicalReport~ findById(UUID)
        +List~MedicalReport~ findByCustomerId(UUID)
        +Optional~MedicalReport~ findLatestByCustomerId(UUID)
    }
    class InMemoryMedicalReportRepository

    class IndemnityAgreementRepository {
        <<interface>>
        +void save(IndemnityAgreement)
        +Optional~IndemnityAgreement~ findByCustomerId(UUID)
    }
    class InMemoryIndemnityAgreementRepository

    class CustomerAvailabilityRepository {
        <<interface>>
        +void setAvailability(UUID, Set~LocalDateTime~)
        +Set~LocalDateTime~ getAvailability(UUID)
    }
    class InMemoryCustomerAvailabilityRepository

    class CustomerAnalysisReportRepository {
        <<interface>>
        +void save(CustomerAnalysisReport)
        +Optional~CustomerAnalysisReport~ findByCustomerId(UUID)
    }
    class InMemoryCustomerAnalysisReportRepository

    InMemoryCustomerRepository ..|> CustomerRepository
    InMemoryAppointmentRepository ..|> AppointmentRepository
    InMemoryStaffRepository ..|> StaffRepository
    InMemoryTimeSlotRepository ..|> TimeSlotRepository
    InMemoryMedicalReportRepository ..|> MedicalReportRepository
    InMemoryIndemnityAgreementRepository ..|> IndemnityAgreementRepository
    InMemoryCustomerAvailabilityRepository ..|> CustomerAvailabilityRepository
    InMemoryCustomerAnalysisReportRepository ..|> CustomerAnalysisReportRepository
```

---

## Diagram 3 — Factory Layer (`factory` package)

> Every model object is created through factory interfaces.
> Default implementations live alongside the interfaces.
> Only SpaceMateApp (composition root) instantiates the default factories.

```mermaid
classDiagram
    direction TB

    class CustomerFactory {
        <<interface>>
        +Customer create(UUID, String, String, String)
    }
    class DefaultCustomerFactory

    class StaffFactory {
        <<interface>>
        +Staff create(UUID, String, StaffRole)
    }
    class DefaultStaffFactory

    class TimeSlotFactory {
        <<interface>>
        +TimeSlot create(UUID, Staff, LocalDateTime, LocalDateTime)
    }
    class DefaultTimeSlotFactory

    class AppointmentFactory {
        <<interface>>
        +Appointment create(UUID, Customer, Staff, TimeSlot, AppointmentType, LocalDateTime)
    }
    class DefaultAppointmentFactory

    class MedicalReportFactory {
        <<interface>>
        +MedicalReport create(UUID, Customer, Appointment, LocalDate)
    }
    class DefaultMedicalReportFactory

    class IndemnityAgreementFactory {
        <<interface>>
        +IndemnityAgreement create(UUID, Customer, LocalDate)
    }
    class DefaultIndemnityAgreementFactory

    class CustomerAnalysisReportFactory {
        <<interface>>
        +CustomerAnalysisReport create(UUID, Customer, LocalDate)
    }
    class DefaultCustomerAnalysisReportFactory

    DefaultCustomerFactory ..|> CustomerFactory
    DefaultStaffFactory ..|> StaffFactory
    DefaultTimeSlotFactory ..|> TimeSlotFactory
    DefaultAppointmentFactory ..|> AppointmentFactory
    DefaultMedicalReportFactory ..|> MedicalReportFactory
    DefaultIndemnityAgreementFactory ..|> IndemnityAgreementFactory
    DefaultCustomerAnalysisReportFactory ..|> CustomerAnalysisReportFactory
```

---

## Diagram 4 — Service Layer (`service` package)

> One service per BPMN subprocess. Each service owns exactly one concern.
> Dependencies are injected via constructor (DIP).
> AppointmentTypeStaffResolver is the Strategy that maps appointment types to qualified staff.

```mermaid
classDiagram
    direction TB

    class NotificationService {
        <<interface>>
        +void sendQuestionnaire(Customer)
        +void sendDocumentList(Customer)
        +void sendReminder(Customer, String)
        +void sendAppointmentConfirmation(Customer, String)
        +void sendMedicalResults(Customer, boolean)
    }
    class MockNotificationService

    class CustomerAnalysisService {
        <<interface>>
        +CustomerAnalysisReport analyse(Customer)
    }
    class MockCustomerAnalysisService

    class AppointmentTypeStaffResolver {
        <<interface>>
        +List~Staff~ findQualifiedStaff(AppointmentType)
    }
    class DefaultAppointmentTypeStaffResolver

    class RegistrationService {
        +Customer registerCustomer(String, String, String, String)
        +void markQuestionnaireCompleted(UUID)
        +List~Customer~ getAllCustomers()
    }

    class IndemnityService {
        +IndemnityAgreement sendAgreement(UUID)
        +void recordSigned(UUID)
        +void sendReminder(UUID)
        +Optional~IndemnityAgreement~ getAgreement(UUID)
    }

    class SchedulingService {
        +List~TimeSlot~ findAvailableSlots(AppointmentType)
        +Appointment scheduleAppointment(UUID, UUID, UUID, AppointmentType)
        +void confirmAppointment(UUID)
        +void cancelAppointment(UUID)
        +List~Appointment~ getAppointmentsForCustomer(UUID)
        +List~TimeSlot~ findAvailableSlotsByStaff(UUID)
        +Set~LocalDateTime~ getCustomerAvailability(UUID)
    }

    class MedicalService {
        +MedicalReport recordInitialMedicalResult(UUID, UUID, boolean, boolean, String)
        +MedicalReport recordFinalMedicalResult(UUID, UUID, boolean, String)
        +Optional~MedicalReport~ getLatestReport(UUID)
    }

    class TrainingService {
        +void completeShuttleBriefing(UUID, UUID)
        +void completeLifestyleCoaching(UUID, UUID)
        +void completeSpecialistConsultation(UUID, UUID)
    }

    class CustomerNotAvailableException {
        -String customerName
        -LocalDateTime requestedTime
    }

    MockNotificationService ..|> NotificationService
    MockCustomerAnalysisService ..|> CustomerAnalysisService
    DefaultAppointmentTypeStaffResolver ..|> AppointmentTypeStaffResolver
    CustomerNotAvailableException --|> RuntimeException

    RegistrationService --> CustomerAnalysisService : requests AI analysis
    RegistrationService --> NotificationService : sends questionnaire
    RegistrationService --> CustomerFactory : creates customers
    IndemnityService --> NotificationService : sends document list
    IndemnityService --> IndemnityAgreementFactory : creates agreements
    SchedulingService --> NotificationService : sends confirmation
    SchedulingService --> AppointmentTypeStaffResolver : resolves qualified staff
    SchedulingService --> AppointmentFactory : creates appointments
    MedicalService --> NotificationService : sends results
    MedicalService --> MedicalReportFactory : creates reports
```

---

## Diagram 5 — Orchestration & UI (`orchestration`, `app`, `ui` packages)

> The orchestrator is the single entry point for the UI.
> It coordinates services and fires events to registered listeners (Observer pattern).
> SpaceMateApp is the composition root — all wiring happens there.

```mermaid
classDiagram
    direction TB

    class OnboardingEventListener {
        <<interface>>
        +void onEvent(OnboardingEvent)
    }

    class OnboardingEvent {
        -OnboardingEventType type
        -Customer customer
        -String message
    }

    class OnboardingEventType {
        <<enumeration>>
        CUSTOMER_REGISTERED
        QUESTIONNAIRE_COMPLETED
        INDEMNITY_SENT
        INDEMNITY_SIGNED
        APPOINTMENT_SUGGESTED
        APPOINTMENT_CONFIRMED
        APPOINTMENT_CANCELLED
        APPOINTMENT_COMPLETED
        MEDICAL_RESULT_RECORDED
        CUSTOMER_APPROVED
        CUSTOMER_REJECTED
        APPOINTMENT_REFUSED
        SIMULATE_RESULT
        STAGE_CHANGED
    }

    class OnboardingOrchestrator {
        +void addEventListener(OnboardingEventListener)
        +void removeEventListener(OnboardingEventListener)
        +Customer registerCustomer(String, String, String, String)
        +void markQuestionnaireCompleted(UUID)
        +Appointment scheduleAppointment(UUID, UUID, UUID, AppointmentType)
        +void confirmAppointment(UUID, UUID)
        +void cancelAppointment(UUID, UUID)
        +void simulateResult(UUID, UUID)
        +int simulateDay(LocalDate)
        +List~AppointmentType~ getRequiredNextAppointments(UUID)
    }

    class SimulatedClock {
        +LocalDate today()
        +void advanceOneDay()
    }

    class SpaceMateApp {
        +void start(Stage)
    }

    class MainController {
        +BorderPane getRoot()
        +void onEvent(OnboardingEvent)
    }

    class WeekCalendarView {
        +void setSelectedCustomer(Customer)
        +void setStaffVisible(UUID, boolean)
        +void refresh()
    }

    OnboardingEvent --> OnboardingEventType
    OnboardingEvent --> Customer

    OnboardingOrchestrator --> RegistrationService
    OnboardingOrchestrator --> IndemnityService
    OnboardingOrchestrator --> SchedulingService
    OnboardingOrchestrator --> MedicalService
    OnboardingOrchestrator --> TrainingService
    OnboardingOrchestrator --> OnboardingEventListener : notifies

    MainController ..|> OnboardingEventListener
    MainController --> OnboardingOrchestrator
    MainController --> SimulatedClock
    MainController o-- WeekCalendarView : aggregation
    WeekCalendarView --> OnboardingOrchestrator

    SpaceMateApp --> OnboardingOrchestrator : constructs
    SpaceMateApp --> MainController : constructs
```
