package com.example.nansy_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button connectBtn;
    private TextView statusText;
    private EditText ipInput;
    private Button transitionBtn;
    private AuthHttpHandler httpHandler;
    private JwtHandler jwtHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectBtn = findViewById(R.id.connectBtn);
        statusText = findViewById(R.id.statusText);
        ipInput = findViewById(R.id.ipInput);
        transitionBtn = findViewById(R.id.transitionBtn);

        jwtHandler = new JwtHandler(this);
        httpHandler = new AuthHttpHandler();
        AuthHttpHandler.init(this);

        if (jwtHandler.jwtIsExists()) {
            statusText.setText("token is exists");
            connectToServer();
        }

        connectBtn.setOnClickListener(v -> {
            connectBtn.setEnabled(false);
            statusText.setText("connecting");

            connectToServer();
        });

        transitionBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        });
    }

    private void connectToServer() {new Thread(() -> {
        try {
            String username = "Dimond";

            AuthHttpHandler.authenticateAndConnect(username);
            String token = AuthHttpHandler.getJwtToken();

            if (token != null && !token.isEmpty()) {
                runOnUiThread(() -> {
                    statusText.setText("Подключено к серверу!");

                    connectBtn.setText("Отправить команду");
                    connectBtn.setEnabled(true);

                    statusText.setText(jwtHandler.getJwtToken());

                    sendCommand();
                });
            } else {
                runOnUiThread(() -> {
                    statusText.setText("Ошибка подключения");
                    connectBtn.setEnabled(true);
                });
            }

        } catch (Exception e) {
            runOnUiThread(() -> {
                statusText.setText("Ошибка: " + e.getMessage());
                connectBtn.setEnabled(true);
            });
        }
    }).start();}

    private void sendCommand() {
        String token = AuthHttpHandler.getJwtToken();
        if (token == null) return;

        // Отправляем команду через WebSocket
        StompWebSocketHandler stomp = new StompWebSocketHandler();
        String serverUrl = ConfigManager.getSystemProperty("websocket.server.url");
        stomp.connect(serverUrl, "dimond", token);

        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        if (stomp.isConnected()) {
            stomp.send("/app/echo", "Hello from Android!");
            statusText.setText("📤 Команда отправлена");
        }
    }
}