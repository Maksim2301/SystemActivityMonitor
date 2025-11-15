package com.example.systemactivitymonitor.repository.interfaces;

import com.example.systemactivitymonitor.model.Report;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторій для роботи зі звітами користувачів.
 */
public interface ReportRepository {

    /** 🟢 Create */
    void save(Report report);

    /** 🔵 Read — фільтр за користувачем і часом */
    List<Report> findByUserIdAndCreatedAtBetween(Integer userId, LocalDateTime start, LocalDateTime end);

    /** 🔵 Read — знайти звіт за ID */
    Optional<Report> findById(Integer id);  // 🆕 Додай цей метод

    /** 🟡 Update */
    void update(Report report);

    /** 🔴 Delete */
    void deleteById(Integer id);
}
