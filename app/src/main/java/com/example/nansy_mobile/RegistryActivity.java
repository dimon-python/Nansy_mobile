package com.example.nansy_mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegistryActivity extends AppCompatActivity {
    private EditText registryUsernameInput;
    private EditText registryPasswordInput;
    private Button registryButton;
    private Button openLoginWindowButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registry);

        registryUsernameInput = findViewById(R.id.registryUsernameInput);
        registryPasswordInput = findViewById(R.id.registryPasswordInput);
        registryButton = findViewById(R.id.registryButton);
        openLoginWindowButton = findViewById(R.id.openLoginWindowButton);

        registryButton.setOnClickListener(v -> {
            registryButton.setClickable(false);
            String username = registryUsernameInput.getText().toString();
            String password = registryPasswordInput.getText().toString();
            new Thread(() -> {
                if (username.trim().isEmpty() || password.trim().isEmpty()) {
                    runOnUiThread(() ->
                            Toast.makeText(this, R.string.no_data_registry_error, Toast.LENGTH_SHORT).show()
                    );
                } else {
                    boolean success = AuthHttpHandler.registry(username, password);
                    runOnUiThread(() -> {
                        if (success) {
                            startActivity(new Intent(this, LoginActivity.class));
                        } else {
                            Toast.makeText(this, R.string.no_registry_error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).start();
        });

        openLoginWindowButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
