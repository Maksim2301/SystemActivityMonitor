package com.example.systemactivitymonitor.factory;

import com.example.systemactivitymonitor.repository.impl.*;
import com.example.systemactivitymonitor.repository.interfaces.*;

public class RepositoryFactory {

    private static final ReportRepository REPORT_REPOSITORY = new ReportRepositoryImpl();
    private static final StatsRepository STATS_REPOSITORY = new StatsRepositoryImpl();
    private static final IdleRepository IDLE_REPOSITORY = new IdleRepositoryImpl();
    private static final UserRepository USER_REPOSITORY = new UserRepositoryImpl();

    public static ReportRepository getReportRepository() {
        return REPORT_REPOSITORY;
    }

    public static StatsRepository getStatsRepository() {
        return STATS_REPOSITORY;
    }

    public static IdleRepository getIdleRepository() {
        return IDLE_REPOSITORY;
    }

    public static UserRepository getUserRepository() {
        return USER_REPOSITORY;
    }
}
