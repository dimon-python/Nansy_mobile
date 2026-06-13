package com.example.nansy_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText loginUsernameInput;
    private EditText loginPasswordInput;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        new ConfigManager(this);
        AuthHttpHandler.init(this);

        loginUsernameInput = findViewById(R.id.loginUsernameInput);
        loginPasswordInput = findViewById(R.id.loginPasswordInput);
        loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(v -> {
            String username = loginUsernameInput.getText().toString();
            String password = loginPasswordInput.getText().toString();
            new Thread(() -> {
                if (username.trim().isEmpty() || password.trim().isEmpty()) {
                    runOnUiThread(() ->
                            Toast.makeText(this, R.string.no_data_login_error, Toast.LENGTH_SHORT).show()
                    );
                } else {
                    boolean success = AuthHttpHandler.login(username, password);
                    runOnUiThread(() -> {
                        if (success) {
                            startActivity(new Intent(this, CommandActivity.class));
                        } else {
                            Toast.makeText(this, R.string.not_logged_error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).start();
        });
    }

}
