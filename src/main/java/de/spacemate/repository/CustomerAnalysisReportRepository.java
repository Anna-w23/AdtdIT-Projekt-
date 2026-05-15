package de.spacemate.repository;

import de.spacemate.model.CustomerAnalysisReport;

import java.util.Optional;
import java.util.UUID;

public interface CustomerAnalysisReportRepository {
    void save(CustomerAnalysisReport report);
    Optional<CustomerAnalysisReport> findByCustomerId(UUID customerId);
}
