package com.example.systemactivitymonitor.service;

import com.example.systemactivitymonitor.model.IdleTime;
import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.repository.impl.UserRepositoryImpl;
import com.example.systemactivitymonitor.repository.interfaces.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class UserService {

    private final UserRepository userRepository = new UserRepositoryImpl();

    // ================================
    // 🔹 Реєстрація / логін / видалення
    // ================================
    public User registerUser(String username, String plainPassword, String email) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Ім'я користувача не може бути порожнім.");
        if (plainPassword == null || plainPassword.isBlank())
            throw new IllegalArgumentException("Пароль не може бути порожнім.");

        User user = new User(username, plainPassword, email);
        userRepository.save(user);
        return user;
    }

    public Optional<User> login(String username, String plainPassword) {
        if (username == null || username.isBlank() || plainPassword == null || plainPassword.isBlank())
            return Optional.empty();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent() && plainPassword.equals(userOpt.get().getPasswordHash()))
            return userOpt;

        return Optional.empty();
    }

    public void deleteUser(Integer id) {
        if (id == null) return;
        userRepository.deleteById(id);
    }

    // ================================
    // 🔹 Зміна пароля
    // ================================
    public void changePassword(User user, String oldPassword, String newPassword) {
        validateUser(user);

        if (!user.getPasswordHash().equals(oldPassword))
            throw new SecurityException("Старий пароль неправильний.");

        userRepository.updatePassword(user.getId(), newPassword);
        user.setPasswordHash(newPassword);
    }

    public boolean verifyPassword(User user, String plainPassword) {
        return user.getPasswordHash().equals(plainPassword);
    }

    public boolean isIdleActive(User user) {
        return getActiveIdle(user).isPresent();
    }

    public Optional<IdleTime> getActiveIdle(User user) {
        return user.getIdleTimes().stream()
                .filter(idle -> idle.getEndTime() == null)
                .findFirst();
    }

    // ================================
    // 🔹 Приватна перевірка
    // ================================
    private void validateUser(User user) {
        if (user == null || user.getId() == null)
            throw new IllegalArgumentException("Користувач не заданий або не збережений (id == null).");
    }
}
