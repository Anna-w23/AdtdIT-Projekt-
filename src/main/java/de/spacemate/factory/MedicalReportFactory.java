package de.spacemate.factory;

import de.spacemate.model.Appointment;
import de.spacemate.model.Customer;
import de.spacemate.model.MedicalReport;

import java.time.LocalDate;
import java.util.UUID;

public interface MedicalReportFactory {
    MedicalReport create(UUID id, Customer customer, Appointment appointment, LocalDate issuedOn);
}
