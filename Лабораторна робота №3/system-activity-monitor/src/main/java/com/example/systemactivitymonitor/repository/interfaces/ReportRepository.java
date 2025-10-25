package com.example.systemactivitymonitor.repository.interfaces;

import com.example.systemactivitymonitor.model.Report;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportRepository {

    void save(Report report);

    Optional<Report> findById(Integer id);

    List<Report> findByUserId(Integer userId);

    List<Report> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<Report> findAll();

    void deleteById(Integer id);

    void delete(Report report);
}
