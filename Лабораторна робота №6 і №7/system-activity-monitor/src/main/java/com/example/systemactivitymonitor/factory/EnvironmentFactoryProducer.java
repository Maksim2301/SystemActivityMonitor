package com.example.systemactivitymonitor.factory;

/**
 * 🏗️ Виробник фабрик — визначає ОС і повертає відповідну реалізацію фабрики.
 */
public class EnvironmentFactoryProducer {

    public static SystemEnvironmentFactory getFactory() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            System.out.println("🪟 Використовується WindowsSystemFactory");
            return new WindowsSystemFactory();
        } else {
            System.out.println("🐧 Використовується LinuxSystemFactory");
            return new LinuxSystemFactory();
        }
    }
}
