package de.spacemate.factory;

import de.spacemate.model.Customer;
import de.spacemate.model.CustomerAnalysisReport;

import java.time.LocalDate;
import java.util.UUID;

public interface CustomerAnalysisReportFactory {
    CustomerAnalysisReport create(UUID id, Customer customer, LocalDate generatedOn);
}
