package de.spacemate.service.mock;

import de.spacemate.factory.CustomerAnalysisReportFactory;
import de.spacemate.model.Customer;
import de.spacemate.model.CustomerAnalysisReport;
import de.spacemate.service.CustomerAnalysisService;

import java.time.LocalDate;
import java.util.UUID;

public class MockCustomerAnalysisService implements CustomerAnalysisService {

    private final CustomerAnalysisReportFactory reportFactory;

    public MockCustomerAnalysisService(CustomerAnalysisReportFactory reportFactory) {
        this.reportFactory = reportFactory;
    }

    @Override
    public CustomerAnalysisReport analyse(Customer customer) {
        CustomerAnalysisReport report = reportFactory.create(
                UUID.randomUUID(),
                customer,
                LocalDate.now()
        );
        report.setSummary("Auto-generated report for " + customer.getFullName()
                + ". No anomalies detected by mock analyser.");
        report.setSpecialistConsultationRecommended(false);
        report.setLifestyleCoachingRecommended(false);
        System.out.println("[AI AGENT] Analysis report generated for " + customer.getFullName());
        return report;
    }
}
