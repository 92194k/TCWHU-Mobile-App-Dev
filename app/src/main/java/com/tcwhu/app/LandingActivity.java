package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
// The Toast import is no longer needed and has been removed.

import androidx.appcompat.app.AppCompatActivity;

public class LandingActivity extends AppCompatActivity {

    private Button buttonStudentLogin;
    private Button buttonStudentSignUp;
    private Button buttonAdminLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        // Find the buttons from our layout
        buttonStudentLogin = findViewById(R.id.buttonStudentLogin);
        buttonStudentSignUp = findViewById(R.id.buttonStudentSignUp);
        buttonAdminLogin = findViewById(R.id.buttonAdminLogin);

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

        // --- THIS IS THE UPDATED PART --- ✅
        // This now opens the Admin Login screen.
        buttonAdminLogin.setOnClickListener(v -> {
            Intent intent = new Intent(LandingActivity.this, AdminLoginActivity.class);
            startActivity(intent);
        });
    }
}