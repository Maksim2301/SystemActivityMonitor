package com.example.systemactivitymonitor.service;

import com.example.systemactivitymonitor.model.IdleTime;
import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.repository.impl.IdleRepositoryImpl;
import com.example.systemactivitymonitor.repository.interfaces.IdleRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * IdleService — тепер працює ТІЛЬКИ у ручному режимі.
 * ✔ Користувач сам вмикає простій (Offline)
 * ✔ Користувач сам вимикає простій (Online)
 * ✔ Ніякого авто-переходу, ніяких діалогів
 * ✔ Без дублювання Idle-сесій
 * ✔ Повністю валідний для БД
 */
public class IdleService {

    private final IdleRepository idleRepository = new IdleRepositoryImpl();

    // ====================================================================================
    // 🟢 Користувач натиснув кнопку "Offline" → запускаємо простій
    // ====================================================================================
    public IdleTime startIdle(User user) {
        validateUser(user);

        if (isIdleActive(user)) {
            System.out.println("ℹ Простій вже активний — повторний запуск ігнорується.");
            return getActiveIdle(user).get();
        }

        IdleTime idle = new IdleTime(user, LocalDateTime.now());
        user.getIdleTimes().add(idle);

        idleRepository.save(idle);

        System.out.println("⏸ Режим OFFLINE увімкнено. Простій стартував о " + idle.getStartTime());
        return idle;
    }

    // ====================================================================================
    // 🔴 Користувач натиснув кнопку "Online" → завершуємо простій
    // ====================================================================================
    public IdleTime endIdle(User user) {
        validateUser(user);

        Optional<IdleTime> activeIdleOpt = getActiveIdle(user);

        if (activeIdleOpt.isEmpty()) {
            System.out.println("ℹ Немає активного простою. ONLINE вже увімкнено.");
            return null;
        }

        IdleTime activeIdle = activeIdleOpt.get();

        LocalDateTime end = LocalDateTime.now();
        long durationSec = Duration.between(activeIdle.getStartTime(), end).getSeconds();

        if (durationSec < 0) durationSec = 0;

        activeIdle.setEndTime(end);
        activeIdle.setDurationSeconds((int) durationSec);

        idleRepository.save(activeIdle);

        System.out.println("✅ Режим ONLINE увімкнено. Простій завершено (" + durationSec + " сек).");

        return activeIdle;
    }

    // ====================================================================================
    // 📘 Перевірка активного простою
    // ====================================================================================
    public boolean isIdleActive(User user) {
        return getActiveIdle(user).isPresent();
    }

    public Optional<IdleTime> getActiveIdle(User user) {
        validateUser(user);
        return user.getIdleTimes().stream()
                .filter(i -> i.getEndTime() == null)
                .findFirst();
    }

    // ====================================================================================
    // 🔐 Перевірка користувача
    // ====================================================================================
    private void validateUser(User user) {
        if (user == null || user.getId() == null)
            throw new IllegalArgumentException("Користувача не задано або він не збережений (id=null).");
    }
}
