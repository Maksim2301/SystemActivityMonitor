package com.example.systemactivitymonitor.service.impl;

import com.example.systemactivitymonitor.agent.ActiveWindowTracker;
import com.example.systemactivitymonitor.agent.KeyboardMouseListener;
import com.example.systemactivitymonitor.agent.SystemMetricsCollector;
import com.example.systemactivitymonitor.model.SystemStats;
import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.repository.impl.StatsRepositoryImpl;
import com.example.systemactivitymonitor.repository.interfaces.StatsRepository;
import com.example.systemactivitymonitor.service.interfaces.MonitoringService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class MonitoringServiceImpl implements MonitoringService {

    private final StatsRepository statsRepository = new StatsRepositoryImpl();

    private final SystemMetricsCollector metricsCollector = new SystemMetricsCollector();
   // private final KeyboardMouseListener inputListener = new KeyboardMouseListener();

    @Override
    public void recordSystemStats(User user,
                                  double cpuLoad,
                                  double memoryUsage,
                                  String activeWindow,
                                  int keyboardPresses,
                                  int mouseClicks) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Користувач не заданий або не збережений (id == null).");
        }
        SystemStats stats = new SystemStats(
                user,
                BigDecimal.valueOf(cpuLoad),
                BigDecimal.valueOf(memoryUsage),
                (activeWindow == null || activeWindow.isBlank()) ? "Unknown Window" : activeWindow,
                keyboardPresses,
                mouseClicks
        );
        statsRepository.save(stats);
    }

    @Override
    public void recordSystemStats(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Користувач не заданий або не збережений (id == null).");
        }
        try {
            double cpu = metricsCollector.getCpuLoad().doubleValue();
            double ram = metricsCollector.getMemoryUsageMb().doubleValue();
            String window = ActiveWindowTracker.getActiveWindowTitle();

            // int keys = inputListener.getKeyPressCount();
            // int clicks = inputListener.getMouseClickCount();
            int keys = 0;
            int clicks = 0;

            if (window == null || window.isBlank()) window = "Unknown Window";

            SystemStats stats = new SystemStats(
                    user,
                    BigDecimal.valueOf(cpu),
                    BigDecimal.valueOf(ram),
                    window,
                    keys,
                    clicks
            );
            statsRepository.save(stats);
            // inputListener.resetCounters();
        } catch (Exception e) {
            System.err.println("[MonitoringService] Помилка при зборі системної статистики: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<SystemStats> getStatsByUser(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Користувач не заданий або не збережений (id == null).");
        }
        return statsRepository.findByUserId(user.getId());
    }

    @Override
    public List<SystemStats> getStatsForPeriod(User user, LocalDateTime start, LocalDateTime end) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Користувач не заданий або не збережений (id == null).");
        }
        if (start == null || end == null || end.isBefore(start)) {
            throw new IllegalArgumentException("Некоректний діапазон дат.");
        }
        return statsRepository.findByUserId(user.getId())
                .stream()
                .filter(s -> s.getRecordedAt() != null
                        && !s.getRecordedAt().isBefore(start)
                        && !s.getRecordedAt().isAfter(end))
                .toList();
    }

    @Override
    public double calculateAverageCpu(User user, LocalDateTime start, LocalDateTime end) {
        List<SystemStats> stats = getStatsForPeriod(user, start, end);
        return stats.stream()
                .map(SystemStats::getCpuLoad)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);
    }

    @Override
    public double calculateAverageRam(User user, LocalDateTime start, LocalDateTime end) {
        List<SystemStats> stats = getStatsForPeriod(user, start, end);
        return stats.stream()
                .map(SystemStats::getMemoryUsageMb)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);
    }
}
