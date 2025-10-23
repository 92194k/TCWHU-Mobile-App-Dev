package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        new Handler(Looper.getMainLooper()).postDelayed(this::checkUserStatus, SPLASH_DELAY);
    }

    private void checkUserStatus() {
        // Get the current user from Firebase Auth
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // No user is signed in, go to the Landing screen
            navigateTo(LandingActivity.class);
        } else {
            // A user is signed in, check their verification status in Firestore
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            Boolean isVerified = task.getResult().getBoolean("isVerified");
                            if (isVerified != null && isVerified) {
                                // User is signed in AND verified, go to the main dashboard
                                navigateTo(StudentHomeActivity.class);
                            } else {
                                // User is signed in but NOT verified, go to the pending screen
                                navigateTo(PendingVerificationActivity.class);
                            }
                        } else {
                            // User is in Auth but not Firestore, go to landing as a safe default
                            navigateTo(LandingActivity.class);
                        }
                    });
        }
    }

    private void navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(SplashActivity.this, activityClass);
        startActivity(intent);
        finish(); // Close the splash activity
    }
}