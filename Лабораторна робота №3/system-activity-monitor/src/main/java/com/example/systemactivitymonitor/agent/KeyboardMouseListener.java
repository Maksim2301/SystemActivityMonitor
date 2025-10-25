package com.example.systemactivitymonitor.agent;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;


public class KeyboardMouseListener implements NativeKeyListener, NativeMouseListener {

    private final AtomicInteger keyPressCount = new AtomicInteger(0);
    private final AtomicInteger mouseClickCount = new AtomicInteger(0);
    private boolean initialized = false;

    public KeyboardMouseListener() {
        disableJNativeHookLogging();

        try {
            String dllPath = extractNativeLibrary();
            if (dllPath != null) {
                System.setProperty("jnativehook.lib.location", dllPath);
            }

            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            GlobalScreen.addNativeMouseListener(this);

            initialized = true;
            System.out.println("KeyboardMouseListener: глобальні події вводу зареєстровані.");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Помилка: неможливо завантажити JNativeHook.dll — " + e.getMessage());
        } catch (NativeHookException e) {
            System.err.println("Помилка реєстрації JNativeHook: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Загальна помилка ініціалізації JNativeHook: " + e.getMessage());
        }
    }

    private String extractNativeLibrary() {
        try {
            String resourcePath = "/com/github/kwhat/jnativehook/lib/windows/x86_64/JNativeHook.dll";
            InputStream dllStream = KeyboardMouseListener.class.getResourceAsStream(resourcePath);

            if (dllStream == null) {
                System.err.println("Не знайдено JNativeHook.dll усередині jar.");
                return null;
            }

            File tempDir = new File(System.getProperty("java.io.tmpdir"), "jnativehook");
            if (!tempDir.exists()) tempDir.mkdirs();

            File dllFile = new File(tempDir, "JNativeHook.dll");
            if (!dllFile.exists()) {
                Files.copy(dllStream, dllFile.toPath());
            }

            return tempDir.getAbsolutePath();
        } catch (IOException e) {
            System.err.println("Помилка копіювання JNativeHook.dll: " + e.getMessage());
            return null;
        }
    }

    private void disableJNativeHookLogging() {
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        keyPressCount.incrementAndGet();
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {}

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {}

    @Override
    public void nativeMouseClicked(NativeMouseEvent e) {
        mouseClickCount.incrementAndGet();
    }

    @Override
    public void nativeMousePressed(NativeMouseEvent e) {}

    @Override
    public void nativeMouseReleased(NativeMouseEvent e) {}

    public int getKeyPressCount() {
        return initialized ? keyPressCount.get() : 0;
    }

    public int getMouseClickCount() {
        return initialized ? mouseClickCount.get() : 0;
    }

    public void resetCounters() {
        keyPressCount.set(0);
        mouseClickCount.set(0);
    }

    public boolean isInitialized() {
        return initialized;
    }
}
