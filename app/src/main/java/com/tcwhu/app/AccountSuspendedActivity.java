package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView; // <-- ADDED

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class AccountSuspendedActivity extends AppCompatActivity {

    private Button buttonLogout;
    private TextView textStatusMessage, textStatusTitle; // <-- ADDED textStatusTitle
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_suspended);

        buttonLogout = findViewById(R.id.buttonLogout);
        textStatusMessage = findViewById(R.id.textStatusMessage);
        textStatusTitle = findViewById(R.id.textStatusTitle); // <-- ADDED
        mAuth = FirebaseAuth.getInstance();

        // Get the message from SplashActivity or LoginActivity
        String message = getIntent().getStringExtra("STATUS_MESSAGE");
        if (message != null && !message.isEmpty()) {
            textStatusMessage.setText(message);

            // --- ADDED: Set a custom title based on the message ---
            if (message.contains("banned")) {
                textStatusTitle.setText("Account Banned");
            } else if (message.contains("suspended")) {
                textStatusTitle.setText("Account Suspended");
            } else if (message.contains("deletion")) {
                textStatusTitle.setText("Deletion Requested");
            }
            // --- END OF ADDED CODE ---
        }

        buttonLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(AccountSuspendedActivity.this, LandingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        buttonLogout.performClick();
        super.onBackPressed();
    }
}