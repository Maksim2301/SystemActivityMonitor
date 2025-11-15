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

    private Long systemUptimeSeconds;
    private BigDecimal diskTotalGb;
    private BigDecimal diskFreeGb;
    private LocalDateTime recordedAt;

    public SystemStats() {}

    public SystemStats(User user, BigDecimal cpuLoad, BigDecimal memoryUsageMb,
                       String activeWindow, Integer keyboardPresses, Integer mouseClicks) {
        this(user, cpuLoad, memoryUsageMb, activeWindow, keyboardPresses, mouseClicks,
                null, null, null);
    }

    public SystemStats(User user,
                       BigDecimal cpuLoad,
                       BigDecimal memoryUsageMb,
                       String activeWindow,
                       Integer keyboardPresses,
                       Integer mouseClicks,
                       Long systemUptimeSeconds,
                       BigDecimal diskTotalGb,
                       BigDecimal diskFreeGb) {

        this.user = user;
        this.cpuLoad = cpuLoad;
        this.memoryUsageMb = memoryUsageMb;
        this.activeWindow = activeWindow;
        this.keyboardPresses = keyboardPresses;
        this.mouseClicks = mouseClicks;
        this.systemUptimeSeconds = systemUptimeSeconds;
        this.diskTotalGb = diskTotalGb;
        this.diskFreeGb = diskFreeGb;
        this.recordedAt = LocalDateTime.now();
    }

    // ===== Гетери / Сетери =====
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

    public Long getSystemUptimeSeconds() { return systemUptimeSeconds; }
    public void setSystemUptimeSeconds(Long systemUptimeSeconds) { this.systemUptimeSeconds = systemUptimeSeconds; }

    public BigDecimal getDiskTotalGb() { return diskTotalGb; }
    public void setDiskTotalGb(BigDecimal diskTotalGb) { this.diskTotalGb = diskTotalGb; }

    public BigDecimal getDiskFreeGb() { return diskFreeGb; }
    public void setDiskFreeGb(BigDecimal diskFreeGb) { this.diskFreeGb = diskFreeGb; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
