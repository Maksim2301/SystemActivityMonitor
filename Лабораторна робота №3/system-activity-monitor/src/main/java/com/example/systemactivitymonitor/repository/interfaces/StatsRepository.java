package com.example.systemactivitymonitor.repository.interfaces;

import com.example.systemactivitymonitor.model.SystemStats;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StatsRepository {

    void save(SystemStats systemStats);

    Optional<SystemStats> findById(Integer id);

    List<SystemStats> findByUserId(Integer userId);

    List<SystemStats> findByRecordedAtBetween(LocalDateTime start, LocalDateTime end);

    List<SystemStats> findByCpuLoadGreaterThan(double threshold);

    List<SystemStats> findAll();

    void deleteById(Integer id);

    void delete(SystemStats systemStats);
}
