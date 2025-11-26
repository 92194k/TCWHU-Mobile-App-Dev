package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LandingActivity extends AppCompatActivity {

    private Button buttonStudentLogin, buttonStudentSignUp, buttonAdminLogin;
    private TextView textPrivacyPolicy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        buttonStudentLogin = findViewById(R.id.buttonStudentLogin);
        buttonStudentSignUp = findViewById(R.id.buttonStudentSignUp);
        buttonAdminLogin = findViewById(R.id.buttonAdminLogin);
        textPrivacyPolicy = findViewById(R.id.textPrivacyPolicy);

        buttonStudentLogin.setOnClickListener(v -> {
            Intent intent = new Intent(LandingActivity.this, StudentLoginActivity.class);
            startActivity(intent);
        });

        buttonStudentSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LandingActivity.this, StudentSignUpActivity.class);
            startActivity(intent);
        });

        buttonAdminLogin.setOnClickListener(v -> {
            Intent intent = new Intent(LandingActivity.this, AdminLoginActivity.class);
            startActivity(intent);
        });

        textPrivacyPolicy.setOnClickListener(v -> {
            Intent intent = new Intent(LandingActivity.this, PrivacyPolicyActivity.class);
            startActivity(intent);
        });
    }
}