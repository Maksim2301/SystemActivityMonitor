package com.example.systemactivitymonitor.service.impl;

import com.example.systemactivitymonitor.model.IdleTime;
import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.repository.impl.IdleRepositoryImpl;
import com.example.systemactivitymonitor.repository.interfaces.IdleRepository;
import com.example.systemactivitymonitor.service.interfaces.IdleService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class IdleServiceImpl implements IdleService {

    private final IdleRepository idleRepository = new IdleRepositoryImpl();

    @Override
    public IdleTime startIdle(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Користувач не заданий або не збережений (id == null).");
        }

        if (user.isIdleActive()) {
            throw new IllegalStateException("Сесія простою вже активна. Завершіть поточну перед новою.");
        }

        IdleTime idle = user.startIdleManually();
        idleRepository.save(idle);
        System.out.println("⏸ Почато простій о " + idle.getStartTime());
        return idle;
    }

    @Override
    public void endIdle(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Користувач не заданий або не збережений (id == null).");
        }

        user.getActiveIdle().ifPresent(idle -> {
            LocalDateTime endTime = LocalDateTime.now();
            idle.setEndTime(endTime);

            long durationSeconds = Duration.between(idle.getStartTime(), endTime).getSeconds();
            idle.setDurationSeconds((int) durationSeconds);

            user.endIdleManually();
            idleRepository.save(idle);
            System.out.println("Простій завершено. Тривалість: " + durationSeconds + " сек.");
        });
    }

    @Override
    public List<IdleTime> getIdleTimesByUser(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Користувач не заданий або не збережений (id == null).");
        }
        return idleRepository.findByUserId(user.getId());
    }

    @Override
    public List<IdleTime> getIdleTimesInPeriod(User user, LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new IllegalArgumentException("Некоректний діапазон дат для простою.");
        }

        return idleRepository.findByStartTimeBetween(start, end)
                .stream()
                .filter(i -> i.getUser() != null && i.getUser().getId().equals(user.getId()))
                .toList();
    }

    @Override
    public long getTotalIdleSeconds(User user, LocalDateTime start, LocalDateTime end) {
        return getIdleTimesInPeriod(user, start, end).stream()
                .filter(i -> i.getDurationSeconds() != null)
                .mapToLong(IdleTime::getDurationSeconds)
                .sum();
    }
}
