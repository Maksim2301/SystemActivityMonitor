//package com.example.systemactivitymonitor.util;
//
//import com.example.systemactivitymonitor.model.User;
//import com.example.systemactivitymonitor.service.impl.MonitoringServiceImpl;
//
///**
// * Планувальник, який періодично записує системні дані користувача
// * (CPU, RAM, активне вікно, клавіатура, миша).
// */
//public class ThreadScheduler {
//
//    private final MonitoringServiceImpl monitoringService;
//    private final User user;
//    private boolean running;
//
//    public ThreadScheduler(MonitoringServiceImpl monitoringService, User user) {
//        this.monitoringService = monitoringService;
//        this.user = user;
//    }
//
//    /** Запускає фонове логування */
//    public void start() {
//        running = true;
//        Thread thread = new Thread(() -> {
//            System.out.println("▶️ Моніторинг системи запущено для користувача: " + user.getUsername());
//            while (running) {
//                try {
//                    monitoringService.recordSystemStats(user);
//                    Thread.sleep(5000); // Кожні 5 секунд
//                } catch (Exception e) {
//                    System.err.println("❌ Помилка моніторингу: " + e.getMessage());
//                }
//            }
//        });
//        thread.setDaemon(true);
//        thread.start();
//    }
//
//    /** Зупиняє моніторинг */
//    public void stop() {
//        running = false;
//        System.out.println("⏹️ Моніторинг зупинено.");
//    }
//}
