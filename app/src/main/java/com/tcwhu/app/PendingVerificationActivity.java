package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView; // Import ImageView

import androidx.appcompat.app.AppCompatActivity;

public class PendingVerificationActivity extends AppCompatActivity {

    private Button buttonBackToHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_verification);

        buttonBackToHome = findViewById(R.id.buttonBackToHome);

        // --- ANIMATION CODE --- ✅
        // Find the clock icon view (assuming ID clockIcon is added to XML)
        ImageView clockIcon = findViewById(R.id.clockIcon);
        Animation rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_clock);
        if (clockIcon != null) {
            clockIcon.startAnimation(rotateAnimation);
        }
        // --- END ANIMATION CODE ---

        buttonBackToHome.setOnClickListener(v -> {
            Intent intent = new Intent(PendingVerificationActivity.this, LandingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}