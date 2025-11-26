package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        Button buttonLogout = findViewById(R.id.buttonLogout);
        GridLayout managementGrid = findViewById(R.id.managementGrid);

        buttonLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(AdminDashboardActivity.this, LandingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Management Cards
        addManagementCard(managementGrid, "Student Verification", R.drawable.ic_check_circle, StudentVerificationActivity.class);
        addManagementCard(managementGrid, "Reports Management", R.drawable.ic_report, ReportsManagementActivity.class);
        addManagementCard(managementGrid, "Events Management", R.drawable.ic_events, EventsManagementActivity.class);
        addManagementCard(managementGrid, "User Overview", R.drawable.ic_users, UserOverviewActivity.class);

        // Bagong Support Inbox Card
        addManagementCard(managementGrid, "Support Inbox", R.drawable.ic_chat, AdminChatListActivity.class);

        addManagementCard(managementGrid, "Activity Logs", R.drawable.ic_logs, ActivityLogsActivity.class);
        addManagementCard(managementGrid, "Admin Settings", R.drawable.ic_settings, AdminSettingsActivity.class);
    }

    private void addManagementCard(GridLayout gridLayout, String title, int iconResId, final Class<?> targetActivity) {
        LayoutInflater inflater = LayoutInflater.from(this);
        MaterialCardView cardView = (MaterialCardView) inflater.inflate(R.layout.card_management_item, gridLayout, false);

        ImageView icon = cardView.findViewById(R.id.card_icon);
        TextView cardTitle = cardView.findViewById(R.id.card_title);

        icon.setImageResource(iconResId);
        cardTitle.setText(title);

        cardView.setOnClickListener(v -> {
            if (targetActivity != null) {
                Intent intent = new Intent(AdminDashboardActivity.this, targetActivity);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Feature not yet implemented.", Toast.LENGTH_SHORT).show();
            }
        });

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setGravity(Gravity.FILL);

        int margin = (int) (8 * getResources().getDisplayMetrics().density);
        params.setMargins(margin, margin, margin, margin);

        cardView.setLayoutParams(params);
        gridLayout.addView(cardView);
    }
}