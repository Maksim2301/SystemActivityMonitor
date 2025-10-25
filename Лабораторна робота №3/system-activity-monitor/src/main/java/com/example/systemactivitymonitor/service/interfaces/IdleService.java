package com.example.systemactivitymonitor.service.interfaces;

import com.example.systemactivitymonitor.model.IdleTime;
import com.example.systemactivitymonitor.model.User;
import java.time.LocalDateTime;
import java.util.List;

public interface IdleService {

    IdleTime startIdle(User user);

    void endIdle(User user);

    List<IdleTime> getIdleTimesByUser(User user);

    List<IdleTime> getIdleTimesInPeriod(User user, LocalDateTime start, LocalDateTime end);

    long getTotalIdleSeconds(User user, LocalDateTime start, LocalDateTime end);
}
