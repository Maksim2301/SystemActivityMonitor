package com.example.systemactivitymonitor.service.interfaces;

import com.example.systemactivitymonitor.model.User;
import java.util.List;
import java.util.Optional;

public interface UserService {

    User registerUser(String username, String plainPassword, String email);

    Optional<User> login(String username, String plainPassword);

    void changePassword(User user, String oldPassword, String newPassword);

    Optional<User> findById(Integer id);

    Optional<User> findByUsername(String username);

    List<User> getAllUsers();

    void deleteUser(Integer id);
}
