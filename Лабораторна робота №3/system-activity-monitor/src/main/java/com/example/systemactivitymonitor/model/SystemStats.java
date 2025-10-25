package com.example.systemactivitymonitor.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SystemStats {

    private Integer id;
    private User user;
    private BigDecimal cpuLoad;
    private BigDecimal memoryUsageMb;
    private String activeWindow;
    private Integer keyboardPresses;
    private Integer mouseClicks;
    private LocalDateTime recordedAt;

    public SystemStats() {}

    public SystemStats(User user, BigDecimal cpuLoad, BigDecimal memoryUsageMb,
                       String activeWindow, Integer keyboardPresses, Integer mouseClicks) {

        this.user = user;
        this.cpuLoad = cpuLoad;
        this.memoryUsageMb = memoryUsageMb;
        this.activeWindow = activeWindow;
        this.keyboardPresses = keyboardPresses;
        this.mouseClicks = mouseClicks;
        this.recordedAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public BigDecimal getCpuLoad() { return cpuLoad; }
    public void setCpuLoad(BigDecimal cpuLoad) { this.cpuLoad = cpuLoad; }
    public BigDecimal getMemoryUsageMb() { return memoryUsageMb; }
    public void setMemoryUsageMb(BigDecimal memoryUsageMb) { this.memoryUsageMb = memoryUsageMb; }
    public String getActiveWindow() { return activeWindow; }
    public void setActiveWindow(String activeWindow) { this.activeWindow = activeWindow; }
    public Integer getKeyboardPresses() { return keyboardPresses; }
    public void setKeyboardPresses(Integer keyboardPresses) { this.keyboardPresses = keyboardPresses; }
    public Integer getMouseClicks() { return mouseClicks; }
    public void setMouseClicks(Integer mouseClicks) { this.mouseClicks = mouseClicks; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
