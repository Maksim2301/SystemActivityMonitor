package com.example.systemactivitymonitor.repository.impl;

import com.example.systemactivitymonitor.model.Report;
import com.example.systemactivitymonitor.repository.interfaces.ReportRepository;
import com.example.systemactivitymonitor.util.DatabaseConnection;
import com.google.gson.Gson;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReportRepositoryImpl implements ReportRepository {

    private final Gson gson = new Gson();

    /** 🟢 CREATE */
    @Override
    public void save(Report report) {
        String sql = """
            INSERT INTO reports 
            (user_id, report_name, period_start, period_end, cpu_avg, ram_avg, 
             idle_time_total_seconds, app_usage_json, file_path, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String appUsageJson = gson.toJson(report.getAppUsagePercent());
            ps.setInt(1, report.getUser().getId());
            ps.setString(2, report.getReportName());
            ps.setDate(3, Date.valueOf(report.getPeriodStart()));
            ps.setDate(4, Date.valueOf(report.getPeriodEnd()));
            ps.setBigDecimal(5, report.getCpuAvg());
            ps.setBigDecimal(6, report.getRamAvg());
            ps.setBigDecimal(7, report.getIdleTimeTotalSeconds());
            ps.setString(8, appUsageJson);
            ps.setString(9, report.getFilePath());
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Помилка при створенні звіту: " + e.getMessage(), e);
        }
    }

    /** 🟡 UPDATE */
    @Override
    public void update(Report report) {
        String sql = """
            UPDATE reports 
            SET report_name=?, period_start=?, period_end=?, cpu_avg=?, ram_avg=?, 
                idle_time_total_seconds=?, app_usage_json=?, file_path=? 
            WHERE id=?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String appUsageJson = gson.toJson(report.getAppUsagePercent());
            ps.setString(1, report.getReportName());
            ps.setDate(2, Date.valueOf(report.getPeriodStart()));
            ps.setDate(3, Date.valueOf(report.getPeriodEnd()));
            ps.setBigDecimal(4, report.getCpuAvg());
            ps.setBigDecimal(5, report.getRamAvg());
            ps.setBigDecimal(6, report.getIdleTimeTotalSeconds());
            ps.setString(7, appUsageJson);
            ps.setString(8, report.getFilePath());
            ps.setInt(9, report.getId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Помилка при оновленні звіту: " + e.getMessage(), e);
        }
    }

    /** 🔵 READ — фільтр за користувачем і датами */
    @Override
    public List<Report> findByUserIdAndCreatedAtBetween(Integer userId, LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT * FROM reports WHERE user_id=? AND created_at BETWEEN ? AND ?";
        return executeQuery(sql, ps -> {
            ps.setInt(1, userId);
            ps.setTimestamp(2, Timestamp.valueOf(start));
            ps.setTimestamp(3, Timestamp.valueOf(end));
        });
    }

    /** 🔴 DELETE */
    @Override
    public void deleteById(Integer id) {
        String sql = "DELETE FROM reports WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при видаленні звіту: " + e.getMessage(), e);
        }
    }

    /** 🔹 Виконання SELECT-запитів */
    private List<Report> executeQuery(String sql, SQLConsumer<PreparedStatement> paramsSetter) {
        List<Report> reports = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (paramsSetter != null) paramsSetter.accept(ps);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) reports.add(mapReport(rs));

        } catch (SQLException e) {
            throw new RuntimeException("Помилка виконання запиту до таблиці reports: " + e.getMessage(), e);
        }
        return reports;
    }

    /** 🔹 Перетворення ResultSet → Report */
    private Report mapReport(ResultSet rs) throws SQLException {
        Report r = new Report();
        r.setId(rs.getInt("id"));
        r.setReportName(rs.getString("report_name"));
        r.setPeriodStart(rs.getDate("period_start").toLocalDate());
        r.setPeriodEnd(rs.getDate("period_end").toLocalDate());
        r.setCpuAvg(rs.getBigDecimal("cpu_avg"));
        r.setRamAvg(rs.getBigDecimal("ram_avg"));
        r.setIdleTimeTotalSeconds(rs.getBigDecimal("idle_time_total_seconds"));

        String json = rs.getString("app_usage_json");
        if (json != null && !json.isBlank()) {
            r.setAppUsagePercent(gson.fromJson(json, Map.class));
        }

        r.setFilePath(rs.getString("file_path"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) r.setCreatedAt(createdAt.toLocalDateTime());
        return r;
    }

    /** 🔹 Функціональний інтерфейс для параметрів PreparedStatement */
    @FunctionalInterface
    private interface SQLConsumer<T> {
        void accept(T t) throws SQLException;
    }
}
