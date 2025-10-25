package com.example.systemactivitymonitor.repository.impl;

import com.example.systemactivitymonitor.model.IdleTime;
import com.example.systemactivitymonitor.repository.interfaces.IdleRepository;
import com.example.systemactivitymonitor.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при збереженні IdleTime: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<IdleTime> findById(Integer id) {
        String sql = "SELECT * FROM idle_time WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                IdleTime idle = mapResultSet(rs);
                return Optional.of(idle);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<IdleTime> findByUserId(Integer userId) {
        List<IdleTime> list = new ArrayList<>();
        String sql = "SELECT * FROM idle_time WHERE user_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<IdleTime> findByStartTimeBetween(LocalDateTime start, LocalDateTime end) {
        List<IdleTime> list = new ArrayList<>();
        String sql = "SELECT * FROM idle_time WHERE start_time BETWEEN ? AND ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(start));
            ps.setTimestamp(2, Timestamp.valueOf(end));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<IdleTime> findByDurationGreaterThan(int duration) {
        List<IdleTime> list = new ArrayList<>();
        String sql = "SELECT * FROM idle_time WHERE duration_seconds > ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, duration);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<IdleTime> findAll() {
        List<IdleTime> list = new ArrayList<>();
        String sql = "SELECT * FROM idle_time";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
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

    @Override
    public void delete(IdleTime idleTime) {
        if (idleTime.getId() != null)
            deleteById(idleTime.getId());
    }

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
