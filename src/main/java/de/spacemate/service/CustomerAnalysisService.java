package de.spacemate.service;

import de.spacemate.model.Customer;
import de.spacemate.model.CustomerAnalysisReport;

/**
 * Generates an analysis report for a newly registered customer.
 * The mock implementation produces a fixed dummy report.
 * A real implementation would call an AI/ML service.
 */
public interface CustomerAnalysisService {
    CustomerAnalysisReport analyse(Customer customer);
}