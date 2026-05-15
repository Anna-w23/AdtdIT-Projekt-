package de.spacemate.repository.inmemory;

import de.spacemate.model.CustomerAnalysisReport;
import de.spacemate.repository.CustomerAnalysisReportRepository;

import java.util.*;

public class InMemoryCustomerAnalysisReportRepository implements CustomerAnalysisReportRepository {

    private final Map<UUID, CustomerAnalysisReport> store = new LinkedHashMap<>();

    @Override
    public void save(CustomerAnalysisReport report) {
        store.put(report.getCustomerId(), report);
    }

    @Override
    public Optional<CustomerAnalysisReport> findByCustomerId(UUID customerId) {
        return Optional.ofNullable(store.get(customerId));
    }
}
