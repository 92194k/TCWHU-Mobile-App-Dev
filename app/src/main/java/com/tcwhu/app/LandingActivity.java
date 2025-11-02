package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView; // Import TextView
import androidx.appcompat.app.AppCompatActivity;

public class LandingActivity extends AppCompatActivity {

    private Button buttonStudentLogin, buttonStudentSignUp, buttonAdminLogin;
    private TextView textPrivacyPolicy; // ADDED

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        // Find the buttons from our layout
        buttonStudentLogin = findViewById(R.id.buttonStudentLogin);
        buttonStudentSignUp = findViewById(R.id.buttonStudentSignUp);
        buttonAdminLogin = findViewById(R.id.buttonAdminLogin);
        textPrivacyPolicy = findViewById(R.id.textPrivacyPolicy); // ADDED

        // This button opens the Login screen
        buttonStudentLogin.setOnClickListener(v -> {
            Intent intent = new Intent(LandingActivity.this, StudentLoginActivity.class);
            startActivity(intent);
        });

        // This button opens the Sign Up screen
        buttonStudentSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LandingActivity.this, StudentSignUpActivity.class);
            startActivity(intent);
        });

        // This opens the Admin Login screen
        buttonAdminLogin.setOnClickListener(v -> {
            Intent intent = new Intent(LandingActivity.this, AdminLoginActivity.class);
            startActivity(intent);
        });

        // ADDED: Listener for the privacy policy text
        textPrivacyPolicy.setOnClickListener(v -> {
            Intent intent = new Intent(LandingActivity.this, PrivacyPolicyActivity.class);
            startActivity(intent);
        });
    }
}