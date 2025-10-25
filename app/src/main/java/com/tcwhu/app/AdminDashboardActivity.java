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

        // --- Student Verification Card (Phase 18) ---
        Button verificationCard = new Button(this);
        verificationCard.setText("Student Verification");
        verificationCard.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, StudentVerificationActivity.class);
            startActivity(intent);
        });
        managementGrid.addView(verificationCard);

        // --- Events Management Card (Phase 19) ---
        Button eventsCard = new Button(this);
        eventsCard.setText("Events Management");
        eventsCard.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, EventsManagementActivity.class);
            startActivity(intent);
        });
        managementGrid.addView(eventsCard);

        // --- Reports Management Card (Phase 20) ---
        Button reportsCard = new Button(this);
        reportsCard.setText("Reports Management");
        reportsCard.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, ReportsManagementActivity.class);
            startActivity(intent);
        });
        managementGrid.addView(reportsCard);

        // --- User Overview Card (Phase 21) ---
        Button userOverviewCard = new Button(this);
        userOverviewCard.setText("User Overview");
        userOverviewCard.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, UserOverviewActivity.class);
            startActivity(intent);
        });
        managementGrid.addView(userOverviewCard);

        // --- Activity Logs Card (Phase 22) ---
        Button logsCard = new Button(this);
        logsCard.setText("Activity Logs");
        logsCard.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, ActivityLogsActivity.class);
            startActivity(intent);
        });
        managementGrid.addView(logsCard);

        // --- Admin Settings Card (Phase 23) --- ✅
        Button settingsCard = new Button(this);
        settingsCard.setText("Admin Settings");
        settingsCard.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminSettingsActivity.class);
            startActivity(intent);
        });
        managementGrid.addView(settingsCard);
    }
}