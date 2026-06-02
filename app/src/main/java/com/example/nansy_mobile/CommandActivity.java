package com.example.nansy_mobile;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class CommandActivity extends AppCompatActivity {
    private ImageButton commandButton;
    private StompWebSocketHandler stomp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_command);

        commandButton = findViewById(R.id.commandButton);

        String serverUrl = ConfigManager.getSystemProperty("websocket.server.url");
        String username = "Dimond";
        String jwtToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaW1vbmQiLCJpYXQiOjE3Nzk3MjA5NjYsImV4cCI6MTc4MjMxMjk2Nn0.C1nzB17pfPwtF46dDT38f6HoKkOJgQDh7YCmFNRLrzE";

        stomp = new StompWebSocketHandler();
        stomp.connect(serverUrl, username, jwtToken);

        commandButton.setOnClickListener(v -> {
            stomp.send("/app/echo", "shutdown");
        });
    }
}
