package com.example.systemactivitymonitor.repository.interfaces;

import com.example.systemactivitymonitor.model.IdleTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IdleRepository {

    void save(IdleTime idleTime);

    Optional<IdleTime> findById(Integer id);

    List<IdleTime> findByUserId(Integer userId);

    List<IdleTime> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    List<IdleTime> findByDurationGreaterThan(int duration);

    List<IdleTime> findAll();

    void deleteById(Integer id);

    void delete(IdleTime idleTime);
}
