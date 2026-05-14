package de.spacemate.factory;

import de.spacemate.model.Customer;
import de.spacemate.model.CustomerAnalysisReport;

import java.time.LocalDate;
import java.util.UUID;

public class DefaultCustomerAnalysisReportFactory implements CustomerAnalysisReportFactory {

    @Override
    public CustomerAnalysisReport create(UUID id, Customer customer, LocalDate generatedOn) {
        return new CustomerAnalysisReport(id, customer, generatedOn);
    }
}
