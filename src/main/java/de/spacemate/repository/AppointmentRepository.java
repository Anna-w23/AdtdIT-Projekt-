package de.spacemate.repository;

import de.spacemate.model.Appointment;
import de.spacemate.model.AppointmentStatus;
import de.spacemate.model.AppointmentType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository {
    void save(Appointment appointment);
    Optional<Appointment> findById(UUID id);
    List<Appointment> findByCustomerId(UUID customerId);
    List<Appointment> findByCustomerIdAndType(UUID customerId, AppointmentType type);
    List<Appointment> findByDateAndStatus(LocalDate date, AppointmentStatus status);
    List<Appointment> findByStatus(AppointmentStatus status);
    List<Appointment> findAll();
}
