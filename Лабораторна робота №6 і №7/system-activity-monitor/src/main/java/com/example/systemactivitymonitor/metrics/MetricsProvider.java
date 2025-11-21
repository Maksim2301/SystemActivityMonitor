package com.example.systemactivitymonitor.metrics;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 🔧 MetricsProvider — "Implementor" у шаблоні Міст (Bridge).
 *
 * Це уніфікований контракт для всіх ОС (Windows, Linux).
 * Він гарантує однакові метрики для будь-якої реалізації.
 *
 * Усі провайдери зобовʼязані повертати однакові ключі:
 *
 *  CPU:
 *    cpuLoad — BigDecimal %
 *
 *  RAM:
 *    ramUsed  — BigDecimal (MB)
 *    ramTotal — BigDecimal (MB)
 *
 *  Disk:
 *    diskUsed  — BigDecimal (GB)
 *    diskFree  — BigDecimal (GB)
 *    diskTotal — BigDecimal (GB)
 *
 *  Input:
 *    keys
 *    clicks
 *    moves
 *    lastActivitySecAgo
 *
 *  System:
 *    activeWindow (String)
 *    osName (String)
 *    uptime (String in format "X d Y h Z m")
 */
public interface MetricsProvider {

    // =============================================================
    // 🔥 CPU
    // =============================================================
    BigDecimal getCpuLoad();

    // =============================================================
    // 🧠 RAM
    // =============================================================
    BigDecimal getRamUsed();
    BigDecimal getRamTotal();

    // =============================================================
    // 💾 Disk
    // =============================================================
    void updateDiskStats();

    BigDecimal getDiskTotal();
    BigDecimal getDiskFree();
    BigDecimal getDiskUsed();

    // =============================================================
    // 🪟 Active window
    // =============================================================
    String getActiveWindowTitle();

    // =============================================================
    // ⏳ Uptime string (days / hours / minutes)
    // =============================================================
    String getUptime();

    // =============================================================
    // ⌨️🖱 Input monitoring
    // =============================================================
    /** Запускає моніторинг користувацької активності */
    void startInputMonitoring();

    /** Зупиняє моніторинг користувацької активності */
    void stopInputMonitoring();

    /** Повертає статистику: keys, clicks, moves, lastActivitySecAgo */
    Map<String, Long> getInputStats();

    /** Секунди з моменту останньої активності */
    long getLastActivitySeconds();

    // =============================================================
    // 📦 Уніфікований метод, який повертає повний пакет метрик
    // =============================================================
    Map<String, Object> collectAllMetrics();
}
