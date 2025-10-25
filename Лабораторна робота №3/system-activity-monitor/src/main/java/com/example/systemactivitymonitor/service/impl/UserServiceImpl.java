package com.example.systemactivitymonitor.service.impl;

import com.example.systemactivitymonitor.model.User;
import com.example.systemactivitymonitor.repository.impl.UserRepositoryImpl;
import com.example.systemactivitymonitor.repository.interfaces.UserRepository;
import com.example.systemactivitymonitor.service.interfaces.UserService;

import java.util.List;
import java.util.Optional;

public class UserServiceImpl implements UserService {

    private final UserRepository userRepository = new UserRepositoryImpl();

    @Override
    public User registerUser(String username, String plainPassword, String email) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Ім'я користувача не може бути порожнім.");
        }
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Пароль не може бути порожнім.");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(plainPassword);
        user.setEmail(email);

        userRepository.save(user);
        return user;
    }

    @Override
    public Optional<User> login(String username, String plainPassword) {
        if (username == null || username.isBlank() || plainPassword == null || plainPassword.isBlank()) {
            return Optional.empty();
        }

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent() && plainPassword.equals(userOpt.get().getPasswordHash())) {
            return userOpt;
        }

        return Optional.empty();
    }

    @Override
    public void changePassword(User user, String oldPassword, String newPassword) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Користувач не заданий або не збережений (id == null).");
        }

        if (!oldPassword.equals(user.getPasswordHash())) {
            throw new SecurityException("Старий пароль неправильний.");
        }

        user.setPasswordHash(newPassword);
        userRepository.save(user);
    }

    @Override
    public Optional<User> findById(Integer id) {
        if (id == null) return Optional.empty();
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        return userRepository.findByUsername(username);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public void deleteUser(Integer id) {
        if (id == null) return;
        userRepository.deleteById(id);
    }
}
