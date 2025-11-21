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
        if (idleTime == null || idleTime.getUser() == null || idleTime.getUser().getId() == null) {
            throw new IllegalArgumentException("IdleTime або user.id == null при збереженні");
        }

        final boolean isUpdate = idleTime.getId() != null;

        String sqlInsert = "INSERT INTO idle_time (user_id, start_time, end_time, duration_seconds) " +
                "VALUES (?, ?, ?, ?)";

        String sqlUpdate = "UPDATE idle_time " +
                "SET user_id = ?, start_time = ?, end_time = ?, duration_seconds = ? " +
                "WHERE id = ?";

        String sql = isUpdate ? sqlUpdate : sqlInsert;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // 1) user_id
            ps.setInt(1, idleTime.getUser().getId());

            // 2) start_time (NOT NULL по схемі)
            if (idleTime.getStartTime() == null) {
                throw new IllegalArgumentException("startTime не може бути null для IdleTime");
            }
            ps.setTimestamp(2, Timestamp.valueOf(idleTime.getStartTime()));

            // 3) end_time (може бути NULL)
            if (idleTime.getEndTime() != null) {
                ps.setTimestamp(3, Timestamp.valueOf(idleTime.getEndTime()));
            } else {
                ps.setNull(3, Types.TIMESTAMP);
            }

            // 4) duration_seconds (може бути NULL)
            if (idleTime.getDurationSeconds() != null) {
                ps.setInt(4, idleTime.getDurationSeconds());
            } else {
                ps.setNull(4, Types.INTEGER);
            }

            // 5) id (тільки для UPDATE)
            if (isUpdate) {
                ps.setInt(5, idleTime.getId());
            }

            ps.executeUpdate();

            // Якщо створено новий запис — отримати згенерований ID
            if (!isUpdate) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        idleTime.setId(keys.getInt(1));
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Помилка при збереженні IdleTime: " + e.getMessage(), e);
        }
    }

    @Override
    public List<IdleTime> findByUserId(Integer userId) {
        List<IdleTime> list = new ArrayList<>();
        String sql = "SELECT * FROM idle_time WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Помилка при пошуку IdleTime за user_id: " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public List<IdleTime> findByUserIdAndStartTimeBetween(Integer userId, LocalDateTime start, LocalDateTime end) {
        List<IdleTime> list = new ArrayList<>();
        String sql = "SELECT * FROM idle_time WHERE user_id = ? AND start_time BETWEEN ? AND ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setTimestamp(2, Timestamp.valueOf(start));
            ps.setTimestamp(3, Timestamp.valueOf(end));

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Помилка при пошуку IdleTime за user_id і діапазоном дат: " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public void deleteById(Integer id) {
        String sql = "DELETE FROM idle_time WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Помилка при видаленні IdleTime: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Перетворює рядок таблиці у об’єкт IdleTime.
     * Поле user тут НЕ заповнюється (залишається null), бо є тільки user_id,
     * а сам User дістається в інших сервісах за потреби.
     */
    private IdleTime mapResultSet(ResultSet rs) throws SQLException {
        IdleTime idle = new IdleTime();
        idle.setId(rs.getInt("id"));

        Timestamp startTs = rs.getTimestamp("start_time");
        if (startTs != null) {
            idle.setStartTime(startTs.toLocalDateTime());
        }

        Timestamp endTs = rs.getTimestamp("end_time");
        if (endTs != null) {
            idle.setEndTime(endTs.toLocalDateTime());
        }

        int duration = rs.getInt("duration_seconds");
        if (!rs.wasNull()) {
            idle.setDurationSeconds(duration);
        } else {
            idle.setDurationSeconds(null);
        }

        // user_id можна зчитати окремо при необхідності, але User об'єкт тут не будуємо
        return idle;
    }
}
