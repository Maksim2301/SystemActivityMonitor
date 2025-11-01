package com.example.systemactivitymonitor.repository.impl;

import com.example.systemactivitymonitor.model.IdleTime;
import com.example.systemactivitymonitor.repository.interfaces.IdleRepository;
import com.example.systemactivitymonitor.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Реалізація IdleRepository з використанням JDBC.
 * Відповідає за взаємодію з таблицею idle_time у базі даних.
 */
public class IdleRepositoryImpl implements IdleRepository {

    @Override
    public void save(IdleTime idleTime) {
        String sql = "INSERT INTO idle_time (user_id, start_time, end_time, duration_seconds) VALUES (?, ?, ?, ?)";
        if (idleTime.getId() != null)
            sql = "UPDATE idle_time SET user_id=?, start_time=?, end_time=?, duration_seconds=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, idleTime.getUser().getId());
            ps.setTimestamp(2, Timestamp.valueOf(idleTime.getStartTime()));
            ps.setTimestamp(3, idleTime.getEndTime() != null ? Timestamp.valueOf(idleTime.getEndTime()) : null);
            ps.setObject(4, idleTime.getDurationSeconds());

            if (idleTime.getId() != null)
                ps.setInt(5, idleTime.getId());

            ps.executeUpdate();

            // Якщо створено новий запис — отримати згенерований ID
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) idleTime.setId(keys.getInt(1));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Помилка при збереженні IdleTime: " + e.getMessage(), e);
        }
    }

    @Override
    public List<IdleTime> findByUserId(Integer userId) {
        List<IdleTime> list = new ArrayList<>();
        String sql = "SELECT * FROM idle_time WHERE user_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapResultSet(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }


    @Override
    public List<IdleTime> findByUserIdAndStartTimeBetween(Integer userId, LocalDateTime start, LocalDateTime end) {
        List<IdleTime> list = new ArrayList<>();
        String sql = "SELECT * FROM idle_time WHERE user_id=? AND start_time BETWEEN ? AND ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setTimestamp(2, Timestamp.valueOf(start));
            ps.setTimestamp(3, Timestamp.valueOf(end));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapResultSet(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void deleteById(Integer id) {
        String sql = "DELETE FROM idle_time WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Перетворює рядок таблиці у об’єкт IdleTime */
    private IdleTime mapResultSet(ResultSet rs) throws SQLException {
        IdleTime idle = new IdleTime();
        idle.setId(rs.getInt("id"));
        idle.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        Timestamp end = rs.getTimestamp("end_time");
        if (end != null) idle.setEndTime(end.toLocalDateTime());
        idle.setDurationSeconds(rs.getInt("duration_seconds"));
        return idle;
    }
}
