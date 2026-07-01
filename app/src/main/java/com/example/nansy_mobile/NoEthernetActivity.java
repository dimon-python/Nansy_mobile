package com.example.nansy_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class NoEthernetActivity extends AppCompatActivity {
    private Button checkEthernetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_ethernet);

        AuthHttpHandler.init(this);
        new ConfigManager(this);

        checkEthernetButton = findViewById(R.id.noEthernetButton);

        checkEthernetButton.setOnClickListener(v -> {
            new Thread(() -> {
                boolean isConnected = AuthHttpHandler.checkConnection();

                runOnUiThread(() -> {
                    System.out.println("button is clicked");
                    if (isConnected) {
                        Intent intent;
                        if (MyApplication.getInstance().hasToken()) {
                            intent = new Intent(this, CommandActivity.class);
                        } else {
                            intent = new Intent(this, LoginActivity.class);
                        }
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, R.string.no_ethernet_toast_message, Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });
    }
}
