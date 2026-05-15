package de.spacemate.service;

import de.spacemate.model.Appointment;
import de.spacemate.model.AppointmentStatus;
import de.spacemate.model.Customer;
import de.spacemate.orchestration.OnboardingOrchestrator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class SimulationAdvancer {

    private final OnboardingOrchestrator orchestrator;
    private final Set<UUID> processedToday = new HashSet<>();

    public SimulationAdvancer(OnboardingOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public boolean processAppointmentsUpTo(LocalDate date, LocalTime cutoff) {
        boolean anyProcessed = false;

        List<Appointment> suggested = orchestrator.findByDateAndStatus(date, AppointmentStatus.SUGGESTED)
                .stream()
                .filter(a -> !processedToday.contains(a.getId()))
                .filter(a -> !a.getTimeSlot().getEnd().toLocalTime().isAfter(cutoff))
                .filter(a -> {
                    Customer c = orchestrator.getAllCustomers().stream()
                            .filter(cu -> cu.getId().equals(a.getCustomerId()))
                            .findFirst().orElse(null);
                    return c != null && orchestrator.isScheduledStage(c.getCurrentStage());
                })
                .toList();
        for (Appointment a : suggested) {
            orchestrator.sendProposalToCustomer(a.getCustomerId(), a.getId());
            processedToday.add(a.getId());
            anyProcessed = true;
        }

        orchestrator.collectPendingResponses();

        List<Appointment> confirmed = orchestrator.findByDateAndStatus(date, AppointmentStatus.CONFIRMED)
                .stream()
                .filter(a -> !processedToday.contains(a.getId()))
                .filter(a -> !a.getTimeSlot().getEnd().toLocalTime().isAfter(cutoff))
                .toList();
        for (Appointment a : confirmed) {
            orchestrator.processAppointmentOutcome(a.getCustomerId(), a.getId());
            processedToday.add(a.getId());
            anyProcessed = true;
        }

        return anyProcessed;
    }

    public void resetDay() {
        processedToday.clear();
    }
}
