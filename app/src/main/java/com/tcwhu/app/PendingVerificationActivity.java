package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class PendingVerificationActivity extends AppCompatActivity {

    private Button buttonBackToHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_verification);

        buttonBackToHome = findViewById(R.id.buttonBackToHome);

        buttonBackToHome.setOnClickListener(v -> {
            // Create an intent to go back to the main landing screen
            Intent intent = new Intent(PendingVerificationActivity.this, LandingActivity.class);
            // These flags clear the activity history, so the user can't press 'back'
            // and get into the sign-up flow again.
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Close this activity
        });
    }
}