package com.example.systemactivitymonitor.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class User {

    private Integer id;
    private String username;
    private String passwordHash;
    private String email;
    private LocalDateTime createdAt;
    private List<IdleTime> idleTimes = new ArrayList<>();

    public User() {
        this.createdAt = LocalDateTime.now();
    }

    public User(String username, String plainTextPassword, String email) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username cannot be empty.");
        if (plainTextPassword == null || plainTextPassword.isBlank())
            throw new IllegalArgumentException("Password cannot be empty.");

        this.username = username;
        this.passwordHash = plainTextPassword;
        this.email = email;
        this.createdAt = LocalDateTime.now();
    }

    public IdleTime startIdleManually() {
        if (isIdleActive()) {
            throw new IllegalStateException("Idle session already active.");
        }
        IdleTime idle = new IdleTime(this, LocalDateTime.now());
        this.idleTimes.add(idle);
        return idle;
    }

    public void endIdleManually() {
        Optional<IdleTime> active = getActiveIdle();
        if (active.isEmpty()) throw new IllegalStateException("No active idle session.");
        active.get().finish(LocalDateTime.now());
    }

    public boolean isIdleActive() {
        return getActiveIdle().isPresent();
    }

    public Optional<IdleTime> getActiveIdle() {
        return this.idleTimes.stream()
                .filter(idle -> !idle.isFinished())
                .findFirst();
    }

    public void changePassword(String oldPlainTextPassword, String newPlainTextPassword) {
        if (!verifyPassword(oldPlainTextPassword))
            throw new SecurityException("Incorrect old password.");
        this.passwordHash = newPlainTextPassword;
    }

    public boolean verifyPassword(String plainTextPassword) {
        return this.passwordHash.equals(plainTextPassword);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<IdleTime> getIdleTimes() { return idleTimes; }
    public void setIdleTimes(List<IdleTime> idleTimes) { this.idleTimes = idleTimes; }
}
