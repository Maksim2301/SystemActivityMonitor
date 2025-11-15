package com.example.systemactivitymonitor.repository.impl;

import com.example.systemactivitymonitor.model.SystemStats;
import com.example.systemactivitymonitor.repository.interfaces.StatsRepository;
import com.example.systemactivitymonitor.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StatsRepositoryImpl implements StatsRepository {

    @Override
    public void save(SystemStats s) {
        String sql = "INSERT INTO system_stats (" +
                "user_id, cpu_load, memory_usage_mb, active_window, " +
                "keyboard_presses, mouse_clicks, " +
                "system_uptime_seconds, disk_total_gb, disk_free_gb, recorded_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, s.getUser().getId());
            stmt.setBigDecimal(2, s.getCpuLoad());
            stmt.setBigDecimal(3, s.getMemoryUsageMb());
            stmt.setString(4, s.getActiveWindow());
            stmt.setInt(5, s.getKeyboardPresses());
            stmt.setInt(6, s.getMouseClicks());

            if (s.getSystemUptimeSeconds() != null)
                stmt.setLong(7, s.getSystemUptimeSeconds());
            else
                stmt.setNull(7, Types.BIGINT);

            if (s.getDiskTotalGb() != null)
                stmt.setBigDecimal(8, s.getDiskTotalGb());
            else
                stmt.setNull(8, Types.DECIMAL);

            if (s.getDiskFreeGb() != null)
                stmt.setBigDecimal(9, s.getDiskFreeGb());
            else
                stmt.setNull(9, Types.DECIMAL);

            if (s.getRecordedAt() == null)
                s.setRecordedAt(LocalDateTime.now());
            stmt.setObject(10, s.getRecordedAt());

            int rows = stmt.executeUpdate();
            System.out.println("✅ SystemStats успішно збережено (" + rows + " рядків).");

        } catch (SQLException e) {
            System.err.println("❌ Помилка при збереженні SystemStats: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** 🔥 Новий метод — повертає статистику певного користувача у часовому діапазоні */
    @Override
    public List<SystemStats> findByUserIdAndRecordedAtBetween(Integer userId, LocalDateTime start, LocalDateTime end) {
        List<SystemStats> list = new ArrayList<>();
        String sql = "SELECT * FROM system_stats WHERE user_id = ? AND recorded_at BETWEEN ? AND ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setTimestamp(2, Timestamp.valueOf(start));
            stmt.setTimestamp(3, Timestamp.valueOf(end));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("❌ Помилка пошуку SystemStats за userId і діапазоном дат: " + e.getMessage());
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

    /** 🔹 Метод для мапінгу ResultSet → SystemStats */
    private SystemStats mapRow(ResultSet rs) throws SQLException {
        SystemStats s = new SystemStats();
        s.setId(rs.getInt("id"));
        s.setCpuLoad(rs.getBigDecimal("cpu_load"));
        s.setMemoryUsageMb(rs.getBigDecimal("memory_usage_mb"));
        s.setActiveWindow(rs.getString("active_window"));
        s.setKeyboardPresses(rs.getInt("keyboard_presses"));
        s.setMouseClicks(rs.getInt("mouse_clicks"));
        s.setSystemUptimeSeconds(rs.getLong("system_uptime_seconds"));
        s.setDiskTotalGb(rs.getBigDecimal("disk_total_gb"));
        s.setDiskFreeGb(rs.getBigDecimal("disk_free_gb"));
        s.setRecordedAt(rs.getTimestamp("recorded_at").toLocalDateTime());
        return s;
    }
}
