package de.spacemate.service;

import de.spacemate.model.*;
import de.spacemate.repository.AppointmentRepository;
import de.spacemate.repository.CustomerRepository;

import java.util.UUID;

public class TrainingService {

    private final CustomerRepository customerRepository;
    private final AppointmentRepository appointmentRepository;

    public TrainingService(CustomerRepository customerRepository,
                           AppointmentRepository appointmentRepository) {
        this.customerRepository = customerRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public void completeSpaceTraining(UUID customerId, UUID appointmentId) {
        completeAppointment(customerId, appointmentId, OnboardingStage.SPACE_TRAINING_COMPLETED);
    }

    public void completeSpecialistConsultation(UUID customerId, UUID appointmentId) {
        completeAppointment(customerId, appointmentId, OnboardingStage.SPECIALIST_COMPLETED);
    }

    private void completeAppointment(UUID customerId, UUID appointmentId, OnboardingStage nextStage) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        appointmentRepository.findById(appointmentId).ifPresent(a -> {
            a.setStatus(AppointmentStatus.COMPLETED);
            appointmentRepository.save(a);
        });

        customer.setCurrentStage(nextStage);
        customerRepository.save(customer);
    }
}
