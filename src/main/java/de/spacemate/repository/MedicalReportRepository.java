package de.spacemate.repository;

import de.spacemate.model.MedicalReport;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicalReportRepository {
    void save(MedicalReport report);
    Optional<MedicalReport> findById(UUID id);
    List<MedicalReport> findByCustomerId(UUID customerId);
    Optional<MedicalReport> findLatestByCustomerId(UUID customerId);
}
