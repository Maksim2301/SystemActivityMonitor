package com.example.systemactivitymonitor.service;

import com.example.systemactivitymonitor.model.IdleTime;
import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.repository.impl.IdleRepositoryImpl;
import com.example.systemactivitymonitor.repository.interfaces.IdleRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    /** 📜 Отримати всі періоди простою користувача */
    public List<IdleTime> getIdleTimesByUser(User user) {
        validateUser(user);
        return idleRepository.findByUserId(user.getId());
    }

    /** ⏱ Отримати всі простої за період */
    public List<IdleTime> getIdleTimesInPeriod(User user, LocalDateTime start, LocalDateTime end) {
        validateUser(user);
        validatePeriod(start, end);
        return idleRepository.findByUserIdAndStartTimeBetween(user.getId(), start, end);
    }

    /** ⏳ Порахувати загальний час простою за період */
    public long getTotalIdleSeconds(User user, LocalDateTime start, LocalDateTime end) {
        return getIdleTimesInPeriod(user, start, end).stream()
                .filter(i -> i.getDurationSeconds() != null)
                .mapToLong(IdleTime::getDurationSeconds)
                .sum();
    }

    /** 🗑 Видалити запис простою */
    public void deleteIdleById(Integer idleId) {
        if (idleId == null)
            throw new IllegalArgumentException("ID простою не може бути порожнім.");
        idleRepository.deleteById(idleId);
        System.out.println("🗑 Простій з ID=" + idleId + " видалено.");
    }

    // =======================================================
    // 🔹 Приватні методи перевірок
    // =======================================================

    private void validateUser(User user) {
        if (user == null || user.getId() == null)
            throw new IllegalArgumentException("Користувач не заданий або не збережений (id == null).");
    }

    private void validatePeriod(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || end.isBefore(start))
            throw new IllegalArgumentException("Некоректний діапазон дат для простою.");
    }
}
