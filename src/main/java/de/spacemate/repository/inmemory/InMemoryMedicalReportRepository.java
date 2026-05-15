package de.spacemate.repository.inmemory;

import de.spacemate.model.MedicalReport;
import de.spacemate.repository.MedicalReportRepository;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryMedicalReportRepository implements MedicalReportRepository {

    private final Map<UUID, MedicalReport> store = new LinkedHashMap<>();

    @Override
    public void save(MedicalReport report) {
        store.put(report.getId(), report);
    }

    @Override
    public Optional<MedicalReport> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<MedicalReport> findByCustomerId(UUID customerId) {
        return store.values().stream()
                .filter(r -> r.getCustomerId().equals(customerId))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<MedicalReport> findLatestByCustomerId(UUID customerId) {
        return store.values().stream()
                .filter(r -> r.getCustomerId().equals(customerId))
                .max(Comparator.comparing(MedicalReport::getIssuedOn));
    }
}
