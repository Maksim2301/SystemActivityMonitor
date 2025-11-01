package com.example.systemactivitymonitor.repository.interfaces;

import com.example.systemactivitymonitor.model.Report;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Репозиторій для роботи зі звітами користувачів.
 * Звіти генеруються на основі статистики (SystemStats, IdleTime).
 */
public interface ReportRepository {

    /** C — Create: створює новий звіт у базі */
    void save(Report report);

    /** R — Read: повертає звіти користувача за певний часовий період */
    List<Report> findByUserIdAndCreatedAtBetween(Integer userId, LocalDateTime start, LocalDateTime end);

    /** U — Update: оновлює існуючий звіт */
    void update(Report report);

    /** D — Delete: видаляє звіт за його ID */
    void deleteById(Integer id);
}
