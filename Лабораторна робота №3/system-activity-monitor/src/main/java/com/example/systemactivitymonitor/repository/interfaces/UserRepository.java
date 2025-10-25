package com.example.systemactivitymonitor.repository.interfaces;

import com.example.systemactivitymonitor.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    void save(User user);

    Optional<User> findById(Integer id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findAll();

    void deleteById(Integer id);

    void delete(User user);
}
