package com.example.systemactivitymonitor.service;

import com.example.systemactivitymonitor.model.IdleTime;
import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.repository.impl.IdleRepositoryImpl;
import com.example.systemactivitymonitor.repository.interfaces.IdleRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * IdleService — об’єднаний сервіс для роботи з періодами простою користувача.
 * Містить усю логіку: запуск, завершення, перевірку та збереження простоїв.
 */
public class IdleService {

    private final IdleRepository idleRepository = new IdleRepositoryImpl();

    /** 🟢 Почати період простою */
    public IdleTime startIdle(User user) {
        validateUser(user);

        if (isIdleActive(user)) {
            throw new IllegalStateException("Сесія простою вже активна. Завершіть поточну перед новою.");
        }

        IdleTime idle = new IdleTime(user, LocalDateTime.now());

        // Додаємо до локального списку користувача
        user.getIdleTimes().add(idle);

        idleRepository.save(idle);
        System.out.println("⏸ Почато простій о " + idle.getStartTime());

        return idle;
    }

    /** 🔴 Завершити поточний простій */
    public IdleTime endIdle(User user) {
        validateUser(user);

        Optional<IdleTime> activeIdleOpt = getActiveIdle(user);
        if (activeIdleOpt.isEmpty()) {
            throw new IllegalStateException("Немає активного простою для завершення.");
        }

        IdleTime activeIdle = activeIdleOpt.get();
        LocalDateTime endTime = LocalDateTime.now();

        if (endTime.isBefore(activeIdle.getStartTime())) {
            throw new IllegalArgumentException("Кінцевий час не може бути раніше початку.");
        }

        long durationSeconds = Duration.between(activeIdle.getStartTime(), endTime).getSeconds();
        if (durationSeconds < 0) durationSeconds = 0;

        activeIdle.setEndTime(endTime);
        activeIdle.setDurationSeconds((int) durationSeconds);

        idleRepository.save(activeIdle);
        System.out.println("✅ Простій завершено. Тривалість: " + durationSeconds + " сек.");

        return activeIdle;
    }

    /** 📘 Перевірка — чи є активний простій у користувача */
    public boolean isIdleActive(User user) {
        return getActiveIdle(user).isPresent();
    }

    /** 📘 Отримати поточний активний простій */
    public Optional<IdleTime> getActiveIdle(User user) {
        return user.getIdleTimes().stream()
                .filter(idle -> idle.getEndTime() == null)
                .findFirst();
    }

    // =======================================================
    // 🔹 Приватні методи перевірок
    // =======================================================

    private void validateUser(User user) {
        if (user == null || user.getId() == null)
            throw new IllegalArgumentException("Користувач не заданий або не збережений (id == null).");
    }
}
