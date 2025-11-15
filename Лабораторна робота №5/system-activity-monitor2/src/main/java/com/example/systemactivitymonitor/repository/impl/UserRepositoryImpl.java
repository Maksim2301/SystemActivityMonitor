package com.example.systemactivitymonitor.repository.impl;

import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.repository.interfaces.UserRepository;
import com.example.systemactivitymonitor.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class UserRepositoryImpl implements UserRepository {

    @Override
    public void save(User user) {
        String sql = user.getId() == null ?
                "INSERT INTO users (username, password_hash, email, created_at) VALUES (?, ?, ?, ?)" :
                "UPDATE users SET username=?, password_hash=?, email=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getEmail());

            if (user.getId() == null) {
                stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            } else {
                stmt.setInt(4, user.getId());
            }

            stmt.executeUpdate();

            if (user.getId() == null) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) user.setId(rs.getInt(1));
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Помилка при збереженні користувача: " + e.getMessage());
        }
    }
    @Override
    public Optional<User> findByUsername(String username) {
        return findSingle("SELECT * FROM users WHERE username=?", username);
    }

    @Override
    public void updatePassword(Integer userId, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPasswordHash);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
            System.out.println("🔒 Пароль користувача ID=" + userId + " оновлено.");
        } catch (SQLException e) {
            throw new RuntimeException("❌ Помилка при оновленні паролю: " + e.getMessage(), e);
        }
    }

    private Optional<User> findSingle(String sql, Object param) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, param);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(mapUser(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public void deleteById(Integer id) {
        if (id == null) return;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM reports WHERE user_id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM system_stats WHERE user_id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM idle_time WHERE user_id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            conn.commit();
            System.out.println("🗑 Дані користувача ID=" + id + " видалено.");

        } catch (Exception e) {
            throw new RuntimeException("❌ Помилка при видаленні користувача: " + e.getMessage(), e);
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setEmail(rs.getString("email"));
        u.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return u;
    }
}
