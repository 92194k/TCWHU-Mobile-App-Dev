package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.GridLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        Button buttonLogout = findViewById(R.id.buttonLogout);
        GridLayout managementGrid = findViewById(R.id.managementGrid);

        // Logout button functionality
        buttonLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(AdminDashboardActivity.this, LandingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // --- Student Verification Card ---
        Button verificationCard = new Button(this);
        verificationCard.setText("Student Verification");
        verificationCard.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, StudentVerificationActivity.class);
            startActivity(intent);
        });
        managementGrid.addView(verificationCard);

        // --- Events Management Card ---
        Button eventsCard = new Button(this);
        eventsCard.setText("Events Management");
        eventsCard.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, EventsManagementActivity.class);
            startActivity(intent);
        });
        managementGrid.addView(eventsCard);

        // --- THIS IS THE NEW PART --- ✅
        // --- Reports Management Card ---
        Button reportsCard = new Button(this);
        reportsCard.setText("Reports Management");
        reportsCard.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, ReportsManagementActivity.class);
            startActivity(intent);
        });
        managementGrid.addView(reportsCard);
    }
}