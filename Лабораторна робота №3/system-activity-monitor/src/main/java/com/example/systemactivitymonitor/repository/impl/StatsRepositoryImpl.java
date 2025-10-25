package com.example.systemactivitymonitor.repository.impl;

import com.example.systemactivitymonitor.model.SystemStats;
import com.example.systemactivitymonitor.repository.interfaces.StatsRepository;
import com.example.systemactivitymonitor.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StatsRepositoryImpl implements StatsRepository {

    @Override
    public void save(SystemStats s) {
        String sql = "INSERT INTO system_stats (user_id, cpu_load, memory_usage_mb, active_window, keyboard_presses, mouse_clicks, recorded_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, s.getUser().getId());
            stmt.setBigDecimal(2, s.getCpuLoad());
            stmt.setBigDecimal(3, s.getMemoryUsageMb());
            stmt.setString(4, s.getActiveWindow());
            stmt.setInt(5, s.getKeyboardPresses());
            stmt.setInt(6, s.getMouseClicks());
            stmt.setTimestamp(7, Timestamp.valueOf(s.getRecordedAt()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Помилка при збереженні SystemStats: " + e.getMessage());
        }
    }

    @Override
    public Optional<SystemStats> findById(Integer id) {
        String sql = "SELECT * FROM system_stats WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("❌ Помилка пошуку SystemStats: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<SystemStats> findByUserId(Integer userId) {
        List<SystemStats> list = new ArrayList<>();
        String sql = "SELECT * FROM system_stats WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("❌ Помилка пошуку SystemStats за userId: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<SystemStats> findByRecordedAtBetween(LocalDateTime start, LocalDateTime end) {
        List<SystemStats> list = new ArrayList<>();
        String sql = "SELECT * FROM system_stats WHERE recorded_at BETWEEN ? AND ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(start));
            stmt.setTimestamp(2, Timestamp.valueOf(end));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("❌ Помилка фільтрації SystemStats: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<SystemStats> findByCpuLoadGreaterThan(double threshold) {
        List<SystemStats> list = new ArrayList<>();
        String sql = "SELECT * FROM system_stats WHERE cpu_load > ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, BigDecimal.valueOf(threshold));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("❌ Помилка пошуку SystemStats за CPU: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<SystemStats> findAll() {
        List<SystemStats> list = new ArrayList<>();
        String sql = "SELECT * FROM system_stats";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("❌ Помилка отримання SystemStats: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void deleteById(Integer id) {
        String sql = "DELETE FROM system_stats WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Помилка видалення SystemStats: " + e.getMessage());
        }
    }

    @Override
    public void delete(SystemStats s) {
        deleteById(s.getId());
    }

    private SystemStats mapRow(ResultSet rs) throws SQLException {
        SystemStats s = new SystemStats();
        s.setCpuLoad(rs.getBigDecimal("cpu_load"));
        s.setMemoryUsageMb(rs.getBigDecimal("memory_usage_mb"));
        s.setActiveWindow(rs.getString("active_window"));
        s.setKeyboardPresses(rs.getInt("keyboard_presses"));
        s.setMouseClicks(rs.getInt("mouse_clicks"));
        return s;
    }
}
