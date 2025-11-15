package com.example.systemactivitymonitor.repository.interfaces;

import com.example.systemactivitymonitor.model.IdleTime;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Репозиторій для роботи з періодами простою користувача.
 * Забезпечує CRUD-операції та запити за користувачем і часом.
 */
public interface IdleRepository {

    /** Створює або оновлює запис простою */
    void save(IdleTime idleTime);

    /** Повертає всі простої конкретного користувача */
    List<IdleTime> findByUserId(Integer userId);

    /** Повертає всі простої конкретного користувача у межах часового діапазону */
    List<IdleTime> findByUserIdAndStartTimeBetween(Integer userId, LocalDateTime start, LocalDateTime end);

    void deleteById(Integer id);
}
