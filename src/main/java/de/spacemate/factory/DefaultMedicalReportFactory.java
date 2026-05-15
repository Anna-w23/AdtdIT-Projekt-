package de.spacemate.factory;

import de.spacemate.model.Appointment;
import de.spacemate.model.Customer;
import de.spacemate.model.MedicalReport;

import java.time.LocalDate;
import java.util.UUID;

public class DefaultMedicalReportFactory implements MedicalReportFactory {

    @Override
    public MedicalReport create(UUID id, Customer customer, Appointment appointment, LocalDate issuedOn) {
        return new MedicalReport(id, customer, appointment, issuedOn);
    }
}
