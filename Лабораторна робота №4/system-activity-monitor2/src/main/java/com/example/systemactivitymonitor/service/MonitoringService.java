package com.example.systemactivitymonitor.service;

import com.example.systemactivitymonitor.model.SystemStats;
import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.repository.impl.StatsRepositoryImpl;
import com.example.systemactivitymonitor.repository.interfaces.StatsRepository;

import java.math.BigDecimal;

/**
 * Сервіс відповідає за збір і збереження системних метрик у базу даних.
 * Вся аналітика (підрахунок середніх значень, побудова звітів) тепер у ReportService.
 */
public class MonitoringService {

    private final StatsRepository statsRepository = new StatsRepositoryImpl();

    /** 🔹 Збереження системної статистики користувача */
    public void recordSystemStats(User user,
                                  double cpuLoad,
                                  double memoryUsage,
                                  String activeWindow,
                                  int keyboardPresses,
                                  int mouseClicks) {

        if (user == null || user.getId() == null)
            throw new IllegalArgumentException("Користувач не заданий або не збережений (id == null).");

        try {
            var metrics = com.example.systemactivitymonitor.agent.AppMonitorManager.getMetricsCollector();

            // 🕒 Отримуємо аптайм, дисковий простір
            String uptimeStr = metrics.getUptime();
            long uptimeSec = parseUptimeToSeconds(uptimeStr);
            BigDecimal diskTotal = metrics.getTotalDiskGb();
            BigDecimal diskFree = metrics.getFreeDiskGb();

            // 🧠 Формуємо об’єкт статистики
            SystemStats stats = new SystemStats(
                    user,
                    BigDecimal.valueOf(cpuLoad),
                    BigDecimal.valueOf(memoryUsage),
                    (activeWindow == null || activeWindow.isBlank()) ? "Unknown Window" : activeWindow,
                    keyboardPresses,
                    mouseClicks,
                    uptimeSec,
                    diskTotal,
                    diskFree
            );

            // 💾 Зберігаємо у базу
            statsRepository.save(stats);

            System.out.printf(
                    "✅ Збережено у БД: CPU=%.2f%% | RAM=%.2fMB | Uptime=%d сек | Disk %.2f/%.2f GB | Window: %s%n",
                    cpuLoad,
                    memoryUsage,
                    uptimeSec,
                    (diskFree != null ? diskFree.doubleValue() : 0),
                    (diskTotal != null ? diskTotal.doubleValue() : 0),
                    activeWindow
            );

        } catch (Exception e) {
            System.err.println("[MonitoringService] ❌ Помилка при збереженні метрик:");
            e.printStackTrace();
        }
    }

    /** 🔹 Допоміжний метод — парсинг аптайму у секунди */
    private long parseUptimeToSeconds(String uptimeStr) {
        if (uptimeStr == null || uptimeStr.equalsIgnoreCase("Unknown")) return 0L;
        uptimeStr = uptimeStr.trim().replaceAll("\\s+", " ");
        String[] parts = uptimeStr.split(" ");
        long days = 0, hours = 0, minutes = 0;
        for (int i = 0; i < parts.length - 1; i++) {
            String value = parts[i].trim();
            String next = parts[i + 1].trim();
            if (value.matches("\\d+")) {
                if (next.equalsIgnoreCase("d")) days = Long.parseLong(value);
                if (next.equalsIgnoreCase("h")) hours = Long.parseLong(value);
                if (next.equalsIgnoreCase("m")) minutes = Long.parseLong(value);
            }
        }
        return days * 86400 + hours * 3600 + minutes * 60;
    }

}
