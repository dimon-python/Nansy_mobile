package com.example.nansy_mobile;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

public class AuthHttpHandler {
    private static StompWebSocketHandler stompHandler;
    private static String serverUrl;
    private static String httpServerUrl;
    private static JwtHandler jwtHandler;
    private static String jwtToken;
    private static Context appContext;
    private static final String LOGIN_ENDPOINT = "/login";
    private final static String REGISTER_ENDPOINT = "/register";
    private final static String VERIFY_ENDPOINT = "/verify";

    public static void init(Context context) {
        appContext = context.getApplicationContext();

        jwtHandler = new JwtHandler(appContext);
        stompHandler = new StompWebSocketHandler();
        httpServerUrl = ConfigManager.getSystemProperty("auth.server.url");
    }

    public static boolean login(String username, String password) {
        try {
            Map<String, String> jsonLoginData = new HashMap<>(); // создаем библиотеку с парами для json
            jsonLoginData.put("username", username);
            jsonLoginData.put("password", password);
            String jsonLogin = new Gson().toJson(jsonLoginData); // превращаем библиотеку в json

            URL loginUrl = new URL(httpServerUrl + LOGIN_ENDPOINT);
            HttpURLConnection httpClient = (HttpURLConnection) loginUrl.openConnection();
            httpClient.setRequestMethod("POST");
            httpClient.setRequestProperty("Content-Type", "application/json");
            httpClient.setDoOutput(true);
            httpClient.setConnectTimeout(10000);
            httpClient.setReadTimeout(10000);

            try (OutputStream output = httpClient.getOutputStream()) {
                output.write(jsonLogin.getBytes());
                output.flush();
            }

            int responseCode = httpClient.getResponseCode();
            if (responseCode == 200) {
                InputStream input = httpClient.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Gson gson = new Gson();
                JsonObject jsonResponse = gson.fromJson(response.toString(), JsonObject.class);
                jwtToken = jsonResponse.get("token").getAsString();
                JwtHandler.setJwtToken(jwtToken);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public static boolean register(String username, String password) {
        try{
            Map<String, String> jsonRegistryData = new HashMap<>();
            jsonRegistryData.put("username", username);
            jsonRegistryData.put("password", password);
            String jsonRegistry = new Gson().toJson(jsonRegistryData);

            URL registryUrl = new URL(httpServerUrl + REGISTER_ENDPOINT);
            HttpURLConnection httpClient = (HttpURLConnection) registryUrl.openConnection();
            httpClient.setRequestMethod("POST");
            httpClient.setRequestProperty("Content-Type", "application/json");
            httpClient.setDoOutput(true);
            httpClient.setConnectTimeout(10000);
            httpClient.setReadTimeout(10000);

            try (OutputStream output = httpClient.getOutputStream()) {
                output.write(jsonRegistry.getBytes());
                output.flush();
            }

            return httpClient.getResponseCode() == 200;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean verify() {
        try{
            jwtToken = JwtHandler.getJwtToken();

            Map<String, String> jsonVerifyData = new HashMap<>();
            jsonVerifyData.put("token", jwtToken);
            String jsonVerify = new Gson().toJson(jsonVerifyData);

            URL verifyUrl = new URL(httpServerUrl + VERIFY_ENDPOINT);
            HttpURLConnection httpClient = (HttpURLConnection) verifyUrl.openConnection();
            httpClient.setRequestMethod("POST");
            httpClient.setRequestProperty("Content-Type", "application/json");
            httpClient.setDoOutput(true);
            httpClient.setConnectTimeout(10000);
            httpClient.setReadTimeout(10000);

            try (OutputStream output = httpClient.getOutputStream()) {
                output.write(jsonVerify.getBytes());
                output.flush();
            }

            return httpClient.getResponseCode() == 200;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
