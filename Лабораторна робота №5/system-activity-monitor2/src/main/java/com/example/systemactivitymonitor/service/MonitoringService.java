package com.example.systemactivitymonitor.service;

import com.example.systemactivitymonitor.metrics.MetricsProvider;
import com.example.systemactivitymonitor.model.SystemStats;
import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.repository.impl.StatsRepositoryImpl;
import com.example.systemactivitymonitor.repository.interfaces.StatsRepository;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 🧩 MonitoringService — абстракція для шаблону Міст.
 * Використовує MetricsProvider для збору метрик незалежно від ОС.
 */
public class MonitoringService {

    protected final StatsRepository statsRepository = new StatsRepositoryImpl();
    protected final MetricsProvider metricsProvider;

    protected ScheduledExecutorService scheduler;
    protected volatile boolean active = false;
    protected User activeUser;

    public MonitoringService(MetricsProvider provider) {
        this.metricsProvider = provider;
    }

    public synchronized void start(User user) {
        if (active) return;
        active = true;
        this.activeUser = user;

        scheduler = Executors.newScheduledThreadPool(2);
        scheduler.scheduleAtFixedRate(this::collectMetrics, 0, 5, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(() -> metricsProvider.updateInputActivity(), 0, 1, TimeUnit.SECONDS);

        System.out.println("MonitoringService: моніторинг запущено.");
    }

    public synchronized void stop() {
        active = false;
        if (scheduler != null && !scheduler.isShutdown()) scheduler.shutdownNow();
        System.out.println("Моніторинг зупинено.");
    }

    protected void collectMetrics() {
        if (!active) return;
        Map<String, Object> data = metricsProvider.collectAllMetrics();
        data.putAll(metricsProvider.getInputStats());

        if (activeUser != null) recordSystemStats(data, activeUser);
    }

    public Map<String, Object> collectFormattedStats() {
        Map<String, Object> data = metricsProvider.collectAllMetrics();
        data.putAll(metricsProvider.getInputStats());
        return data;
    }

    protected void recordSystemStats(Map<String, Object> data, User user) {
        try {
            BigDecimal diskTotal = (BigDecimal) data.get("diskTotal");
            BigDecimal diskUsed = (BigDecimal) data.get("diskUsed");
            BigDecimal diskFree = diskTotal.subtract(diskUsed);
            long uptimeSec = parseUptimeToSeconds((String) data.get("uptime"));

            SystemStats stats = new SystemStats(
                    user,
                    (BigDecimal) data.get("cpu"),
                    (BigDecimal) data.get("ram"),
                    (String) data.get("window"),
                    ((Long) data.get("keys")).intValue(),
                    ((Long) data.get("clicks")).intValue(),
                    uptimeSec,
                    diskTotal,
                    diskFree
            );

            statsRepository.save(stats);
        } catch (Exception e) {
            System.err.println("[MonitoringService] Помилка при збереженні метрик: " + e.getMessage());
        }
    }

    public void recordSystemStats(User user) {
        recordSystemStats(collectFormattedStats(), user);
    }

    private long parseUptimeToSeconds(String uptimeStr) {
        if (uptimeStr == null || uptimeStr.equalsIgnoreCase("Unknown")) return 0L;
        uptimeStr = uptimeStr.trim().replaceAll("\\s+", " ");
        String[] parts = uptimeStr.split(" ");
        long days = 0, hours = 0, minutes = 0;
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].matches("\\d+")) {
                switch (parts[i + 1].toLowerCase()) {
                    case "d": days = Long.parseLong(parts[i]); break;
                    case "h": hours = Long.parseLong(parts[i]); break;
                    case "m": minutes = Long.parseLong(parts[i]); break;
                }
            }
        }
        return days * 86400 + hours * 3600 + minutes * 60;
    }

    public String formatStatusSaved() {
        return "Статистика вручну оновлена (" + LocalTime.now().withNano(0) + ")";
    }
}
