package com.example.nansy_mobile;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import android.content.Context;

public class JwtHandler {
    private static String jwtToken;
    private static final String prefsJwtToken = "jwt_token";
    private final Gson gson;

    public JwtHandler(Context context) {
        this.gson = new Gson();
    }

    public String parseJwtToken(String responseBody) {
        try {
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            return jsonResponse.get("token").getAsString();
        } catch (Exception e) {
            System.err.println("Error jwt token");
            return null;
        }
    }

    public boolean jwtIsExists() {
        String token = ConfigManager.getUserProperty(prefsJwtToken);
        return token != null && !token.isEmpty();
    }

    public String getJwtToken() {
        return ConfigManager.getUserProperty(prefsJwtToken);
    }

    public void setJwtToken(String token) {
        if (token == null || token.isEmpty()) {
            System.err.println("Token is null");
            return;
        }
        ConfigManager.setUserProperty(prefsJwtToken, token);
    }
}
