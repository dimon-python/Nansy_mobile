package com.example.nansy_mobile;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        navigateToNext();
    }

    private void navigateToNext() {
        Intent intent;

        if (!MyApplication.isConnected()) {
            intent = new Intent(this, NoEthernetActivity.class);
        } else if (!MyApplication.hasToken()) {
            intent = new Intent(this, LoginActivity.class);
        } else {
            if (AuthHttpHandler.verify()){
                intent = new Intent(this, CommandActivity.class);
            } else {
                intent = new Intent(this, LoginActivity.class);
            }
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
