package com.example.systemactivitymonitor.repository.interfaces;

import com.example.systemactivitymonitor.model.SystemStats;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Репозиторій для збору системної статистики:
 * CPU, RAM, активне вікно, введення користувача тощо.
 */
public interface StatsRepository {

    /** Зберігає новий запис системної статистики */
    void save(SystemStats systemStats);

    /** Повертає статистику конкретного користувача в межах заданого періоду */
    List<SystemStats> findByUserIdAndRecordedAtBetween(Integer userId, LocalDateTime start, LocalDateTime end);

    /** Видаляє запис статистики за ID */
    void deleteById(Integer id);
}
