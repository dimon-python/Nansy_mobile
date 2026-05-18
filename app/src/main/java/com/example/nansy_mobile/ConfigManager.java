package com.example.nansy_mobile;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {
    private static Context appContext;
    private static Properties systemProps = new Properties();
    private static Properties userProps = new Properties();
    private static File userPropertiesFile;

    public ConfigManager(Context context) {
        appContext = context.getApplicationContext();

        userPropertiesFile = new File(appContext.getFilesDir(), "nansy.user.properties");

        try (InputStream input = appContext.getAssets().open("nansy.properties")) {
            systemProps.load(input);
        } catch (IOException e) {
            System.err.println("Не удалось загрузить системные настройки: " + e.getMessage());
        }

        if (userPropertiesFile.exists()) {
            try (FileInputStream input = new FileInputStream(userPropertiesFile)) {
                userProps.load(input);
            } catch (IOException e) {
                System.err.println("Не удалось загрузить пользовательские настройки: " + e.getMessage());
            }
        }
    }

    public static String getUserProperty(String key) {
        String value = userProps.getProperty(key);
        if (value == null) {
            System.err.println("Ошибка получения значения пользовательской конфигурации");
        }

        return value;
    }

    public static String getSystemProperty(String key) {
        String value = systemProps.getProperty(key);
        if(value == null) {
            System.err.println("Ошибка получения значения системной конфигурации");
        }

        return value;
    }

    public static void setUserProperty(String key, String value) {
        userProps.setProperty(key, value);
        try (FileOutputStream output = new FileOutputStream(userPropertiesFile)) {
            userProps.store(output, null);
        } catch (IOException e) {
            System.err.println("Файл пользовательской конфигурации не найден или не доступен: " + e);
        }
    }
}
