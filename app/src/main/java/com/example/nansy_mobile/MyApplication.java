package com.example.nansy_mobile;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;

public class MyApplication extends Application {
    private static MyApplication instance;
    private static boolean isConnected = false;
    private static boolean tokenIsExisted = false;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        new ConfigManager(this);
        AuthHttpHandler.init(this);
        new JwtHandler(this);

        new Thread(() -> {
            isConnected = AuthHttpHandler.checkConnection();
            tokenIsExisted = JwtHandler.jwtIsExists();
        }).start();
    }

    public static MyApplication getInstance() {
        return instance;
    }

    public static boolean isConnected() {
        return isConnected;
    }

    public static boolean hasToken() {
        return tokenIsExisted;
    }
}
