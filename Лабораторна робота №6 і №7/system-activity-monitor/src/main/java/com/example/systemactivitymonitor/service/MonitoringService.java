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
 * MonitoringService — базова "Abstraction" у Bridge pattern.
 * ✔ Захист від винятків у задачах
 * ✔ Кастомний ThreadFactory
 * ✔ Безпечні конвертери (BigDecimal, String, Long)
 * ✔ Підтримує нову структуру метрик
 * ✔ Працює з новими полями SystemStats
 */
public class MonitoringService {

    protected final StatsRepository statsRepository = new StatsRepositoryImpl();
    protected final MetricsProvider metricsProvider;

    protected ScheduledExecutorService scheduler;
    protected volatile boolean active = false;
    protected User activeUser;


    // =======================================================================
    // THREAD FACTORY
    // =======================================================================
    private final ThreadFactory threadFactory = runnable -> {
        Thread t = new Thread(runnable);
        t.setName("MonitoringScheduler-" + t.getId());
        t.setDaemon(true);
        t.setUncaughtExceptionHandler((thr, ex) ->
                System.err.println("❗ Uncaught exception in " + thr.getName() + ": " + ex)
        );
        return t;
    };

    public MonitoringService(MetricsProvider provider) {
        this.metricsProvider = provider;
    }

    // =======================================================================
    // START
    // =======================================================================
    public synchronized void start(User user) {
        if (active) return;

        active = true;
        this.activeUser = user;

        scheduler = Executors.newScheduledThreadPool(2, threadFactory);

        // 1️⃣ системні метрики кожні 5 сек
        scheduler.scheduleAtFixedRate(() -> safeGuard(this::collectMetrics),
                0, 5, TimeUnit.SECONDS);

        // 2️⃣ запуск моніторингу введення (Windows / Linux реалізує сам)
        metricsProvider.startInputMonitoring();

        System.out.println("MonitoringService: моніторинг запущено.");
    }

    // =======================================================================
    // STOP
    // =======================================================================
    public synchronized void stop() {
        active = false;

        metricsProvider.stopInputMonitoring();

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

        System.out.println("Моніторинг зупинено.");
    }

    // =======================================================================
    // SAFE WRAPPER
    // =======================================================================
    protected void safeGuard(Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            System.err.println("⚠ Exception in scheduled task: " + e.getMessage());
        }
    }

    // =======================================================================
    // METRICS COLLECTION
    // =======================================================================
    protected void collectMetrics() {
        if (!active) return;

        Map<String, Object> data = metricsProvider.collectAllMetrics();

        if (activeUser != null) {
            recordSystemStats(data, activeUser);
        }
    }

    public Map<String, Object> collectFormattedStats() {
        return metricsProvider.collectAllMetrics();
    }

    // =======================================================================
    // SAVE METRICS
    // =======================================================================
    protected void recordSystemStats(Map<String, Object> data, User user) {
        try {
            BigDecimal cpu = toDecimal(data.get("cpuLoad"));
            BigDecimal ramUsed = toDecimal(data.get("ramUsed"));
            BigDecimal ramTotal = toDecimal(data.get("ramTotal"));

            BigDecimal diskTotal = toDecimal(data.get("diskTotal"));
            BigDecimal diskFree = toDecimal(data.get("diskFree"));
            BigDecimal diskUsed = toDecimal(data.get("diskUsed"));

            long uptimeSec = parseUptimeToSeconds(safeStr(data.get("uptime")));
            long keys = toLong(data.get("keys"));
            long clicks = toLong(data.get("clicks"));
            long moves = toLong(data.get("moves"));

            String window = safeStr(data.get("activeWindow"));

            SystemStats stats = new SystemStats();
            stats.setUser(user);
            stats.setCpuLoad(cpu);

            stats.setRamUsedMb(ramUsed);
            stats.setRamTotalMb(ramTotal);

            stats.setActiveWindow(window);
            stats.setKeyboardPresses((int) keys);
            stats.setMouseClicks((int) clicks);
            stats.setMouseMoves(moves);

            stats.setSystemUptimeSeconds(uptimeSec);

            stats.setDiskTotalGb(diskTotal);
            stats.setDiskFreeGb(diskFree);
            stats.setDiskUsedGb(diskUsed);

            statsRepository.save(stats);

        } catch (Exception e) {
            System.err.println("[MonitoringService] Помилка збереження метрик: " + e.getMessage());
        }
    }

    // =======================================================================
    // UTILITIES (safe converters)
    // =======================================================================
    protected BigDecimal toDecimal(Object obj) {
        if (obj instanceof BigDecimal bd) return bd;

        if (obj instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }

        try {
            return new BigDecimal(String.valueOf(obj));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    protected long toLong(Object obj) {
        if (obj instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(obj));
        } catch (Exception e) {
            return 0L;
        }
    }

    protected String safeStr(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    protected long parseUptimeToSeconds(String uptime) {
        if (uptime == null || uptime.equalsIgnoreCase("Unknown")) return 0;

        uptime = uptime.trim();
        long days = 0, hours = 0, minutes = 0;

        String[] parts = uptime.split(" ");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].matches("\\d+")) {
                switch (parts[i + 1].toLowerCase()) {
                    case "d" -> days = Long.parseLong(parts[i]);
                    case "h" -> hours = Long.parseLong(parts[i]);
                    case "m" -> minutes = Long.parseLong(parts[i]);
                }
            }
        }

        return days * 86400 + hours * 3600 + minutes * 60;
    }
    public void saveNow(User user) {
        if (user == null) {
            System.out.println("Guest mode — не зберігаємо.");
            return;
        }

        try {
            Map<String, Object> data = collectFormattedStats();
            recordSystemStats(data, user);
        } catch (Exception e) {
            System.err.println("[MonitoringService] Помилка при ручному збереженні: " + e.getMessage());
        }
    }


    public String formatStatusSaved() {
        return "Статистика вручну оновлена (" + LocalTime.now().withNano(0) + ")";
    }
}
