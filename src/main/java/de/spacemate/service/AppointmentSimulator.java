package de.spacemate.service;

import de.spacemate.model.Appointment;

import java.util.UUID;

public interface AppointmentSimulator {
    SimulationResult simulate(UUID customerId, Appointment appointment);
}
