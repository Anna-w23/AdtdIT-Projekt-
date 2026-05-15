package de.spacemate.service;

import de.spacemate.factory.MedicalReportFactory;
import de.spacemate.model.*;
import de.spacemate.repository.AppointmentRepository;
import de.spacemate.repository.CustomerRepository;
import de.spacemate.repository.MedicalReportRepository;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public class MedicalService {

    private final CustomerRepository customerRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalReportRepository medicalReportRepository;
    private final MedicalReportFactory medicalReportFactory;

    public MedicalService(CustomerRepository customerRepository,
                          AppointmentRepository appointmentRepository,
                          MedicalReportRepository medicalReportRepository,
                          MedicalReportFactory medicalReportFactory) {
        this.customerRepository = customerRepository;
        this.appointmentRepository = appointmentRepository;
        this.medicalReportRepository = medicalReportRepository;
        this.medicalReportFactory = medicalReportFactory;
    }

    public MedicalReport recordInitialMedicalResult(UUID customerId,
                                                    UUID appointmentId,
                                                    Map<SpecialistArea, Boolean> specialistResults,
                                                    boolean needsExtendedTraining,
                                                    String remarks) {
        Customer customer = getCustomer(customerId);
        Appointment appointment = getAppointment(appointmentId);

        MedicalReport report = medicalReportFactory.create(
                UUID.randomUUID(), customer, appointment, LocalDate.now());
        specialistResults.forEach(report::setSpecialistResult);
        report.setNeedsExtendedTraining(needsExtendedTraining);
        report.setFlightEligible(true);
        report.setRemarks(remarks);
        medicalReportRepository.save(report);

        markAppointmentCompleted(appointment);
        customer.setCurrentStage(OnboardingStage.FIRST_MEDICAL_COMPLETED);
        customerRepository.save(customer);

        return report;
    }

    public MedicalReport recordFinalMedicalResult(UUID customerId,
                                                  UUID appointmentId,
                                                  boolean flightEligible,
                                                  String remarks) {
        Customer customer = getCustomer(customerId);
        Appointment appointment = getAppointment(appointmentId);

        MedicalReport report = medicalReportFactory.create(
                UUID.randomUUID(), customer, appointment, LocalDate.now());
        report.setFlightEligible(flightEligible);
        report.setRemarks(remarks);
        medicalReportRepository.save(report);

        markAppointmentCompleted(appointment);

        if (flightEligible) {
            customer.setCurrentStage(OnboardingStage.APPROVED);
        } else {
            customer.setCurrentStage(OnboardingStage.FINAL_MEDICAL_COMPLETED);
        }
        customerRepository.save(customer);

        return report;
    }

    private void markAppointmentCompleted(Appointment appointment) {
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);
    }

    private Appointment getAppointment(UUID appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
    }

    private Customer getCustomer(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
    }
}
