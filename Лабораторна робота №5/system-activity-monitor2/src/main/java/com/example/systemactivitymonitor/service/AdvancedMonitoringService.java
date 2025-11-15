package com.example.systemactivitymonitor.service;

import com.example.systemactivitymonitor.metrics.MetricsProvider;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * ⚙️ AdvancedMonitoringService — розширена абстракція (Refined Abstraction).
 * Додає аналітичну логіку: попередження при високому CPU, RAM і заповненні диску.
 */
public class AdvancedMonitoringService extends MonitoringService {

    public AdvancedMonitoringService(MetricsProvider provider) {
        super(provider);
    }

    @Override
    protected void collectMetrics() {
        if (!active) return;

        Map<String, Object> data = metricsProvider.collectAllMetrics();
        data.putAll(metricsProvider.getInputStats());

        // 🧮 Отримуємо основні метрики
        BigDecimal cpu = (BigDecimal) data.get("cpu");
        BigDecimal ram = (BigDecimal) data.get("ram");
        BigDecimal diskTotal = (BigDecimal) data.get("diskTotal");
        BigDecimal diskUsed = (BigDecimal) data.get("diskUsed");
        BigDecimal diskFree = diskTotal.subtract(diskUsed);

        // ======================================================
        // ⚠️ Аналітичні перевірки
        // ======================================================

        // 🔥 1. CPU > 90%
        if (cpu.compareTo(BigDecimal.valueOf(90)) > 0) {
            System.out.printf("⚠️ Попередження: Високе навантаження CPU — %.2f%%%n", cpu);
        }

        // 🧠 2. RAM > 80% використаної
        BigDecimal ramUsagePercent = ram.divide(ram.add(BigDecimal.ONE), 2, RoundingMode.HALF_UP); // приблизна модель
        if (ramUsagePercent.compareTo(BigDecimal.valueOf(0.8)) > 0) {
            System.out.printf("⚠️ Попередження: Високе використання пам'яті — %.2f MB%n", ram);
        }

        // 💾 3. Диск < 10% вільного місця
        BigDecimal freePercent = diskFree.divide(diskTotal, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        if (freePercent.compareTo(BigDecimal.valueOf(10)) < 0) {
            System.out.printf("⚠️ Попередження: Мало вільного місця на диску — %.2f%% залишилось%n", freePercent);
        }

        // ======================================================
        // 💾 Збереження у БД, якщо користувач активний
        // ======================================================
        if (activeUser != null) {
            recordSystemStats(data, activeUser);
        }
    }
}
