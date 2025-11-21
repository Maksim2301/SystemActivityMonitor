package com.example.systemactivitymonitor.service.calculations;

import com.example.systemactivitymonitor.model.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReportCalculator {

    // ------------------------------------------------------------
    // 🧮 Середнє значення
    // ------------------------------------------------------------
    public BigDecimal average(List<SystemStats> stats, Function<SystemStats, BigDecimal> mapper) {
        if (stats == null || stats.isEmpty()) return BigDecimal.ZERO;

        List<BigDecimal> values = stats.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .toList();

        if (values.isEmpty()) return BigDecimal.ZERO;

        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------
    // 🕒 Загальний час простою
    // ------------------------------------------------------------
    public BigDecimal totalIdle(List<IdleTime> idleList) {
        if (idleList == null || idleList.isEmpty()) return BigDecimal.ZERO;
        long sum = idleList.stream()
                .map(IdleTime::getDurationSeconds)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();
        return BigDecimal.valueOf(sum);
    }

    // ------------------------------------------------------------
    // 📊 Відсоток використання застосунків
    // ------------------------------------------------------------
    public Map<String, BigDecimal> appUsagePercent(List<SystemStats> stats) {
        if (stats == null || stats.isEmpty()) return Map.of();

        long total = stats.size();

        Map<String, Long> counts = stats.stream()
                .map(SystemStats::getActiveWindow)
                .filter(Objects::nonNull)
                .map(this::normalizeAppName)
                .collect(Collectors.groupingBy(s -> s, LinkedHashMap::new, Collectors.counting()));

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        counts.forEach((k, v) -> {
            BigDecimal percent = BigDecimal.valueOf(v * 100.0 / total)
                    .setScale(2, RoundingMode.HALF_UP);
            result.put(k, percent);
        });

        return result;
    }

    // ------------------------------------------------------------
    // 📅 Деталізація по днях + годинах
    // ------------------------------------------------------------
    public List<DaySummary> buildDaySummary(List<SystemStats> stats) {
        if (stats == null || stats.isEmpty()) return List.of();

        Map<LocalDate, Map<Integer, List<SystemStats>>> grouped =
                stats.stream()
                        .filter(s -> s.getRecordedAt() != null)
                        .collect(Collectors.groupingBy(
                                s -> s.getRecordedAt().toLocalDate(),
                                Collectors.groupingBy(s -> s.getRecordedAt().getHour())
                        ));

        return grouped.entrySet().stream()
                .map(day -> new DaySummary(
                        day.getKey(),
                        day.getValue().entrySet().stream()
                                .map(h -> new HourStat(
                                        h.getKey(),
                                        average(h.getValue(), SystemStats::getCpuLoad),
                                        average(h.getValue(), SystemStats::getRamUsedMb)   // ✅ FIX HERE
                                ))
                                .sorted(Comparator.comparing(HourStat::getHour))
                                .toList()
                ))
                .sorted(Comparator.comparing(DaySummary::getDate))
                .toList();
    }

    // ------------------------------------------------------------
    // ⏳ Середній аптайм за день
    // ------------------------------------------------------------
    public BigDecimal averageUptime(List<SystemStats> stats) {
        if (stats == null || stats.isEmpty()) return BigDecimal.ZERO;

        Map<LocalDate, Long> dayMaxUptime = stats.stream()
                .filter(s -> s.getRecordedAt() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getRecordedAt().toLocalDate(),
                        Collectors.mapping(
                                s -> Optional.ofNullable(s.getSystemUptimeSeconds()).orElse(0L),
                                Collectors.maxBy(Long::compareTo)
                        )
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().orElse(0L)
                ));

        BigDecimal sumHours = dayMaxUptime.values().stream()
                .map(v -> BigDecimal.valueOf(v / 3600.0))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sumHours.divide(BigDecimal.valueOf(dayMaxUptime.size()), 2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------
    // 🏷 Нормалізація назв вікон
    // ------------------------------------------------------------
    private String normalizeAppName(String t) {
        if (t == null) return "Unknown";
        String s = t.toLowerCase();
        if (s.contains("chrome")) return "Google Chrome";
        if (s.contains("firefox")) return "Mozilla Firefox";
        if (s.contains("edge")) return "Microsoft Edge";
        if (s.contains("opera")) return "Opera Browser";
        if (s.contains("word")) return "MS Word";
        if (s.contains("excel")) return "MS Excel";
        if (s.contains("idea")) return "IntelliJ IDEA";
        if (s.contains("studio")) return "Android Studio";
        if (s.contains("telegram")) return "Telegram";
        if (s.contains("viber")) return "Viber";
        return t.length() > 40 ? t.substring(0, 40) + "..." : t;
    }
}
