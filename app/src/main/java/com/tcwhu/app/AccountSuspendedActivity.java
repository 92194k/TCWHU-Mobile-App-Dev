package com.tcwhu.app;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class AccountSuspendedActivity extends AppCompatActivity {

    private ImageView imageStatusIcon;
    private TextView textStatusTitle;
    private TextView textStatusMessage;
    private Button buttonLogout;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();

        // --- 1. Create the Main Layout Programmatically ---
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.WHITE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(60, 60, 60, 60);
        scrollView.addView(layout);

        // --- 2. Create UI Elements ---

        // Icon
        imageStatusIcon = new ImageView(this);
        imageStatusIcon.setImageResource(android.R.drawable.ic_dialog_alert); // Built-in android icon
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(200, 200);
        iconParams.setMargins(0, 0, 0, 50);
        imageStatusIcon.setLayoutParams(iconParams);
        layout.addView(imageStatusIcon);

        // Title
        textStatusTitle = new TextView(this);
        textStatusTitle.setTextSize(24);
        textStatusTitle.setTypeface(null, Typeface.BOLD);
        textStatusTitle.setTextColor(Color.parseColor("#212121")); // Dark Gray
        textStatusTitle.setGravity(Gravity.CENTER);
        textStatusTitle.setPadding(0, 0, 0, 30);
        layout.addView(textStatusTitle);

        // Message
        textStatusMessage = new TextView(this);
        textStatusMessage.setTextSize(16);
        textStatusMessage.setTextColor(Color.parseColor("#757575")); // Gray
        textStatusMessage.setGravity(Gravity.CENTER);
        textStatusMessage.setPadding(0, 0, 0, 60);
        layout.addView(textStatusMessage);

        // Admin Contact Info
        TextView contactInfo = new TextView(this);
        contactInfo.setText("To appeal, please contact admin at:\nalaokhemberly@gmail.com");
        contactInfo.setTextSize(14);
        contactInfo.setTextColor(Color.parseColor("#673AB7")); // Deep Purple
        contactInfo.setGravity(Gravity.CENTER);
        contactInfo.setPadding(0, 0, 0, 80);
        layout.addView(contactInfo);

        // Logout Button
        buttonLogout = new Button(this);
        buttonLogout.setText("Back to Login");
        buttonLogout.setBackgroundColor(Color.parseColor("#673AB7")); // Deep Purple
        buttonLogout.setTextColor(Color.WHITE);
        buttonLogout.setPadding(40, 20, 40, 20);
        layout.addView(buttonLogout);

        // --- 3. Set the Content View ---
        setContentView(scrollView);

        // --- 4. Logic Implementation ---
        String message = getIntent().getStringExtra("STATUS_MESSAGE");
        if (message != null && !message.isEmpty()) {
            updateUI(message);
        } else {
            // Fallback default
            textStatusTitle.setText("Access Restricted");
            textStatusMessage.setText("Your account status is currently under review.");
        }

        buttonLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, LandingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void updateUI(String message) {
        textStatusMessage.setText(message);
        String lowerMsg = message.toLowerCase();

        if (lowerMsg.contains("deletion")) {
            textStatusTitle.setText("Deletion Pending");
            imageStatusIcon.setColorFilter(Color.parseColor("#FF9800")); // Orange
        } else if (lowerMsg.contains("banned")) {
            textStatusTitle.setText("Account Banned");
            imageStatusIcon.setColorFilter(Color.parseColor("#D32F2F")); // Red
        } else if (lowerMsg.contains("suspended")) {
            textStatusTitle.setText("Account Suspended");
            imageStatusIcon.setColorFilter(Color.parseColor("#F57C00")); // Orange
        } else {
            textStatusTitle.setText("Access Denied");
            imageStatusIcon.setColorFilter(Color.GRAY);
        }
    }

    @Override
    public void onBackPressed() {
        // Prevent bypassing the screen
        buttonLogout.performClick();
    }
}