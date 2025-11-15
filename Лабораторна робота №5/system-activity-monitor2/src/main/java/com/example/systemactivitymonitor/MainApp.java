package com.example.systemactivitymonitor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Scene scene = new Scene(loader.load(), 900, 600);
        stage.setTitle("System Activity Monitor");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
    }

    public static void main(String[] args) {
        try {
            String nativeLibPath = System.getProperty("user.dir") + File.separator + "native_libs";
            File nativeDir = new File(nativeLibPath);
            if (!nativeDir.exists()) {
                boolean created = nativeDir.mkdirs();
                if (created) {
                    System.out.println("Створено теку для нативних бібліотек: " + nativeLibPath);
                }
            }

            System.setProperty("jnativehook.lib.location", nativeLibPath);
            System.out.println("JNativeHook буде розпаковувати DLL у: " + nativeLibPath);

        } catch (Exception e) {
            System.err.println("Помилка під час підготовки теки для JNativeHook: " + e.getMessage());
        }

        launch(args);
    }
}
