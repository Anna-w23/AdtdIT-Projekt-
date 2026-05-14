package de.spacemate.app;

import de.spacemate.factory.*;
import de.spacemate.orchestration.OnboardingOrchestrator;
import de.spacemate.repository.*;
import de.spacemate.repository.inmemory.*;
import de.spacemate.service.*;
import de.spacemate.service.mock.*;
import de.spacemate.ui.CustomerSpawner;
import de.spacemate.ui.DataSeeder;
import de.spacemate.ui.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SpaceMateApp extends Application {

    @Override
    public void start(Stage primaryStage) {

        // --- Repositories ---
        CustomerRepository customerRepo = new InMemoryCustomerRepository();
        StaffRepository staffRepo = new InMemoryStaffRepository();
        TimeSlotRepository timeSlotRepo = new InMemoryTimeSlotRepository();
        AppointmentRepository appointmentRepo = new InMemoryAppointmentRepository();
        MedicalReportRepository medicalReportRepo = new InMemoryMedicalReportRepository();
        IndemnityAgreementRepository indemnityRepo = new InMemoryIndemnityAgreementRepository();
        CustomerAvailabilityRepository availabilityRepo = new InMemoryCustomerAvailabilityRepository();
        StaffAvailabilityRepository staffAvailabilityRepo = new InMemoryStaffAvailabilityRepository();
        ResourceRepository resourceRepo = new InMemoryResourceRepository();
        ResourceAvailabilityRepository resourceAvailabilityRepo = new InMemoryResourceAvailabilityRepository();
        ResourceAssignmentRepository resourceAssignmentRepo = new InMemoryResourceAssignmentRepository();
        DocumentRepository documentRepo = new InMemoryDocumentRepository();
        DocumentAttachmentRepository documentAttachmentRepo = new InMemoryDocumentAttachmentRepository();

        // --- Factories ---
        CustomerFactory customerFactory = new DefaultCustomerFactory();
        StaffFactory staffFactory = new DefaultStaffFactory();
        AppointmentFactory appointmentFactory = new DefaultAppointmentFactory();
        MedicalReportFactory medicalReportFactory = new DefaultMedicalReportFactory();
        IndemnityAgreementFactory indemnityAgreementFactory = new DefaultIndemnityAgreementFactory();
        ResourceFactory resourceFactory = new DefaultResourceFactory();

        // --- Mocked external services ---
        SimulationConfig simulationConfig = new SimulationConfig();
        SimulatedClock clock = new SimulatedClock();

        CustomerCommunicationService communicationService =
                new MockCustomerCommunicationService(customerRepo, documentRepo, availabilityRepo, clock, simulationConfig);
        DocumentAnalysisService analysisService =
                new MockDocumentAnalysisService(documentRepo);
        AppointmentReportService appointmentReportService =
                new MockAppointmentReportService(documentRepo);

        // --- Domain services ---
        AppointmentTypeStaffResolver staffResolver = new DefaultAppointmentTypeStaffResolver(staffRepo);
        StaffRoomResolver staffRoomResolver = new DefaultStaffRoomResolver();
        ResourceRequirementResolver resourceRequirementResolver = new DefaultResourceRequirementResolver();
        ResourceAssignmentService resourceAssignmentService = new ResourceAssignmentService(
                resourceRepo, resourceAvailabilityRepo, resourceAssignmentRepo,
                resourceRequirementResolver, staffRoomResolver);

        RegistrationService registrationService = new RegistrationService(
                customerRepo, documentRepo, communicationService, analysisService, customerFactory);
        IndemnityService indemnityService = new IndemnityService(
                customerRepo, indemnityRepo, indemnityAgreementFactory, communicationService);
        SchedulingService schedulingService = new SchedulingService(
                customerRepo, timeSlotRepo, appointmentRepo, availabilityRepo,
                staffAvailabilityRepo, staffRepo,
                staffResolver, appointmentFactory, new DefaultTimeSlotFactory());
        MedicalService medicalService = new MedicalService(
                customerRepo, appointmentRepo, medicalReportRepo, medicalReportFactory);
        TrainingService trainingService = new TrainingService(
                customerRepo, appointmentRepo);

        // --- Orchestrator ---
        OnboardingOrchestrator orchestrator = new OnboardingOrchestrator(
                customerRepo, medicalReportRepo, appointmentRepo, documentRepo,
                documentAttachmentRepo, registrationService, indemnityService,
                schedulingService, medicalService, trainingService,
                resourceAssignmentService, communicationService, appointmentReportService);

        // --- Seed staff and resources ---
        new DataSeeder(staffRepo, staffAvailabilityRepo, resourceRepo, resourceAvailabilityRepo,
                staffRoomResolver, staffFactory, resourceFactory).seed();

        // --- Customer spawner ---
        CustomerSpawner spawner = new CustomerSpawner(orchestrator, clock);

        // --- UI ---
        MainController controller = new MainController(orchestrator, staffRepo, resourceRepo, clock, spawner, simulationConfig);

        Scene scene = new Scene(controller.getRoot(), 1200, 800);
        primaryStage.setTitle("SpaceMate – Tourist Onboarding");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
