package de.spacemate.repository.inmemory;

import de.spacemate.model.Appointment;
import de.spacemate.model.AppointmentStatus;
import de.spacemate.model.AppointmentType;
import de.spacemate.repository.AppointmentRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class InMemoryAppointmentRepository implements AppointmentRepository {

    private final Map<UUID, Appointment> store = new LinkedHashMap<>();

    @Override
    public void save(Appointment appointment) {
        store.put(appointment.getId(), appointment);
    }

    @Override
    public Optional<Appointment> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Appointment> findByCustomerId(UUID customerId) {
        return store.values().stream()
                .filter(a -> a.getCustomerId().equals(customerId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Appointment> findByCustomerIdAndType(UUID customerId, AppointmentType type) {
        return store.values().stream()
                .filter(a -> a.getCustomerId().equals(customerId) && a.getType() == type)
                .collect(Collectors.toList());
    }

    @Override
    public List<Appointment> findByDateAndStatus(LocalDate date, AppointmentStatus status) {
        return store.values().stream()
                .filter(a -> a.getStatus() == status
                        && a.getScheduledAt().toLocalDate().equals(date))
                .collect(Collectors.toList());
    }

    @Override
    public List<Appointment> findByStatus(AppointmentStatus status) {
        return store.values().stream()
                .filter(a -> a.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<Appointment> findAll() {
        return new ArrayList<>(store.values());
    }
}
