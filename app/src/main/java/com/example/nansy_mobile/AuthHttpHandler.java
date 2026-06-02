package com.example.nansy_mobile;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.HttpURLConnection;

public class AuthHttpHandler {
    private static StompWebSocketHandler stompHandler;
    private static String serverUrl;
    private static URL authUrl;
    private static JwtHandler jwtHandler;
    private static String jwtToken;
    private static Context appContext;

    public static void init(Context context) {
        appContext = context.getApplicationContext();

        jwtHandler = new JwtHandler(appContext);
        stompHandler = new StompWebSocketHandler();
    }

    public static boolean login(String login, String password) {

    }

//    public static boolean register(String login, String password) {
//
//    }

//    public static boolean verify(String jwtToken) {
//
//    }

    public static void authenticateAndConnect(String pcUsername) {

        String authUrlStr = ConfigManager.getSystemProperty("auth.server.url");
        if (authUrlStr == null) {
            System.err.println("error load auth url");
            return;
        }

        try {
            authUrl = new URL(authUrlStr);
            String json = String.format(
                    "{\"username\":\"%s\"}",
                    pcUsername
            );
            HttpURLConnection httpClient = (HttpURLConnection) authUrl.openConnection();
            httpClient.setRequestMethod("POST");
            httpClient.setRequestProperty("Content-Type", "application/json");
            httpClient.setDoOutput(true);
            httpClient.setConnectTimeout(10000);
            httpClient.setReadTimeout(10000);

            try (OutputStream output = httpClient.getOutputStream()) {
                output.write(json.getBytes());
                output.flush();
            }

            int responseCode = httpClient.getResponseCode();
            System.out.println("Response code: " + responseCode);

            BufferedReader reader;
            if (responseCode == 200) {
                reader = new BufferedReader(new InputStreamReader(httpClient.getInputStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(httpClient.getErrorStream()));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            System.out.println("Response body: " + response.toString());

            if (responseCode == 200) {
                jwtToken = jwtHandler.parseJwtToken(response.toString());
                jwtHandler.setJwtToken(jwtToken);
                System.out.println("✅ Token saved");
                System.out.println("   jwtToken: " + jwtToken);
            } else {
                System.err.println("❌ HTTP error: " + responseCode);
                return;
            }

            httpClient.disconnect();

        } catch (Exception e) {
            System.err.println("❌ Connection error: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        jwtToken = jwtHandler.getJwtToken();
        if (jwtToken == null || jwtToken.isEmpty()) {
            System.err.println("❌ No token for WebSocket");
            return;
        }

        serverUrl = ConfigManager.getSystemProperty("websocket.server.url");
        if (serverUrl == null) {
            System.err.println("error load ws server url");
            return;
        }

        stompHandler.connect(serverUrl, pcUsername, jwtToken);
        stompHandler.subscribe("/topic/echo", message -> {
            System.out.println("📩 Echo: " + message);
        });

        stompHandler.send("app/echo", "Hello, world");
    }
}
