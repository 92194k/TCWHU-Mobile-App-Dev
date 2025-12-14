package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LandingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        Button buttonStudentLogin = findViewById(R.id.buttonStudentLogin);
        Button buttonStudentSignUp = findViewById(R.id.buttonStudentSignUp);
        Button buttonAdminLogin = findViewById(R.id.buttonAdminLogin);
        TextView textPrivacyPolicy = findViewById(R.id.textPrivacyPolicy);

        buttonStudentLogin.setOnClickListener(v -> startActivity(new Intent(LandingActivity.this, StudentLoginActivity.class)));
        buttonStudentSignUp.setOnClickListener(v -> startActivity(new Intent(LandingActivity.this, StudentSignUpActivity.class)));
        buttonAdminLogin.setOnClickListener(v -> startActivity(new Intent(LandingActivity.this, AdminLoginActivity.class)));
        textPrivacyPolicy.setOnClickListener(v -> startActivity(new Intent(LandingActivity.this, PrivacyPolicyActivity.class)));
    }
}