package com.example.systemactivitymonitor.service.interfaces;

import com.example.systemactivitymonitor.model.SystemStats;
import com.example.systemactivitymonitor.model.User;

import java.time.LocalDateTime;
import java.util.List;

public interface MonitoringService {

    void recordSystemStats(User user,
                           double cpuLoad,
                           double memoryUsage,
                           String activeWindow,
                           int keyboardPresses,
                           int mouseClicks);

    void recordSystemStats(User user);

    List<SystemStats> getStatsByUser(User user);

    List<SystemStats> getStatsForPeriod(User user, LocalDateTime start, LocalDateTime end);

    double calculateAverageCpu(User user, LocalDateTime start, LocalDateTime end);

    double calculateAverageRam(User user, LocalDateTime start, LocalDateTime end);
}
