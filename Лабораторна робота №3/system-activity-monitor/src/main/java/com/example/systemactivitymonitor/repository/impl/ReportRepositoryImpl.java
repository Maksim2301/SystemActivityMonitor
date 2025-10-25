package com.example.systemactivitymonitor.repository.impl;

import com.example.systemactivitymonitor.model.Report;
import com.example.systemactivitymonitor.repository.interfaces.ReportRepository;
import com.example.systemactivitymonitor.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReportRepositoryImpl implements ReportRepository {

    @Override
    public void save(Report report) {
        String sql = "INSERT INTO reports (user_id, report_name, period_start, period_end, cpu_avg, ram_avg, idle_time_total_seconds, browser_usage_percent, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        if (report.getId() != null)
            sql = "UPDATE reports SET report_name=?, period_start=?, period_end=?, cpu_avg=?, ram_avg=?, idle_time_total_seconds=?, browser_usage_percent=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (report.getId() == null) {
                ps.setInt(1, report.getUser().getId());
                ps.setString(2, report.getReportName());
                ps.setDate(3, Date.valueOf(report.getPeriodStart()));
                ps.setDate(4, Date.valueOf(report.getPeriodEnd()));
                ps.setBigDecimal(5, report.getCpuAvg());
                ps.setBigDecimal(6, report.getRamAvg());
                ps.setBigDecimal(7, report.getIdleTimeTotalSeconds());
                ps.setBigDecimal(8, report.getBrowserUsagePercent());
                ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            } else {
                ps.setString(1, report.getReportName());
                ps.setDate(2, Date.valueOf(report.getPeriodStart()));
                ps.setDate(3, Date.valueOf(report.getPeriodEnd()));
                ps.setBigDecimal(4, report.getCpuAvg());
                ps.setBigDecimal(5, report.getRamAvg());
                ps.setBigDecimal(6, report.getIdleTimeTotalSeconds());
                ps.setBigDecimal(7, report.getBrowserUsagePercent());
                ps.setInt(8, report.getId());
            }
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<Report> findById(Integer id) {
        String sql = "SELECT * FROM reports WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Report report = new Report();
                report.setId(rs.getInt("id"));
                report.setReportName(rs.getString("report_name"));
                report.setCpuAvg(rs.getBigDecimal("cpu_avg"));
                report.setRamAvg(rs.getBigDecimal("ram_avg"));
                report.setIdleTimeTotalSeconds(rs.getBigDecimal("idle_time_total_seconds"));
                report.setBrowserUsagePercent(rs.getBigDecimal("browser_usage_percent"));
                return Optional.of(report);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Report> findByUserId(Integer userId) {
        List<Report> list = new ArrayList<>();
        String sql = "SELECT * FROM reports WHERE user_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Report r = new Report();
                r.setId(rs.getInt("id"));
                r.setReportName(rs.getString("report_name"));
                r.setCpuAvg(rs.getBigDecimal("cpu_avg"));
                r.setRamAvg(rs.getBigDecimal("ram_avg"));
                r.setIdleTimeTotalSeconds(rs.getBigDecimal("idle_time_total_seconds"));
                list.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Report> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
        List<Report> list = new ArrayList<>();
        String sql = "SELECT * FROM reports WHERE created_at BETWEEN ? AND ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(start));
            ps.setTimestamp(2, Timestamp.valueOf(end));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Report r = new Report();
                r.setId(rs.getInt("id"));
                r.setReportName(rs.getString("report_name"));
                list.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Report> findAll() {
        List<Report> list = new ArrayList<>();
        String sql = "SELECT * FROM reports";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Report r = new Report();
                r.setId(rs.getInt("id"));
                r.setReportName(rs.getString("report_name"));
                list.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void deleteById(Integer id) {
        String sql = "DELETE FROM reports WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(Report report) {
        deleteById(report.getId());
    }
}
