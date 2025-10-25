package com.example.systemactivitymonitor.agent;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

public class ActiveWindowTracker {

    public static String getActiveWindowTitle() {
        try {
            char[] buffer = new char[1024];
            WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
            User32.INSTANCE.GetWindowText(hwnd, buffer, 1024);
            return Native.toString(buffer);
        } catch (Exception e) {
            return "Unknown (error reading window)";
        }
    }

    public static void main(String[] args) throws InterruptedException {
        while (true) {
            System.out.println("Активне вікно: " + getActiveWindowTitle());
            Thread.sleep(2000);
        }
    }
}
