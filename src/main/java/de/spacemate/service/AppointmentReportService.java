package de.spacemate.service;

import de.spacemate.model.AppointmentType;
import de.spacemate.model.Document;
import de.spacemate.model.MedicalReport;

import java.util.UUID;

public interface AppointmentReportService {
    Document createInitialMedicalReport(UUID customerId, MedicalReport medicalReport);
    Document createSpecialistReport(UUID customerId, UUID appointmentId, AppointmentType type, boolean passed);
    Document createTrainingReport(UUID customerId, UUID appointmentId, boolean passed);
    Document createFinalMedicalReport(UUID customerId, MedicalReport medicalReport);
}
