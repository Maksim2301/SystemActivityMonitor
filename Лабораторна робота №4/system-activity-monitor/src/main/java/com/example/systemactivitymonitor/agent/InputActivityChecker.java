package com.example.systemactivitymonitor.agent;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.POINT;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Лічильник активності користувача: клавіатура, кліки, рух миші.
 * Підтримує глобальне використання в багатопотоковому режимі.
 */
public class InputActivityChecker {

    private static final int VK_LBUTTON = 0x01;
    private static final int VK_RBUTTON = 0x02;

    private final AtomicInteger keyPressCount = new AtomicInteger(0);
    private final AtomicInteger mouseClickCount = new AtomicInteger(0);
    private final AtomicLong mouseMoveCount = new AtomicLong(0);

    private int lastX = -1;
    private int lastY = -1;

    public synchronized void checkInput() {
        User32 user32 = User32.INSTANCE;

        // 🔹 Клавіатура
        for (int i = 0x08; i <= 0xFE; i++) {
            short state = user32.GetAsyncKeyState(i);
            if ((state & 0x0001) != 0) {
                keyPressCount.incrementAndGet();
            }
        }

        if ((user32.GetAsyncKeyState(VK_LBUTTON) & 0x0001) != 0)
            mouseClickCount.incrementAndGet();
        if ((user32.GetAsyncKeyState(VK_RBUTTON) & 0x0001) != 0)
            mouseClickCount.incrementAndGet();

        POINT point = new POINT();
        user32.GetCursorPos(point);
        int currentX = point.x;
        int currentY = point.y;

        if (lastX != -1 && lastY != -1) {
            if (currentX != lastX || currentY != lastY) {
                mouseMoveCount.incrementAndGet();
            }
        }

        lastX = currentX;
        lastY = currentY;
    }

    public int getKeyPressCount() { return keyPressCount.get(); }
    public int getMouseClickCount() { return mouseClickCount.get(); }
    public long getMouseMoveCount() { return mouseMoveCount.get(); }

    public void resetCounters() {
        keyPressCount.set(0);
        mouseClickCount.set(0);
        mouseMoveCount.set(0);
    }
}
