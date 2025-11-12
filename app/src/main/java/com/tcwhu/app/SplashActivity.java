package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        new Handler(Looper.getMainLooper()).postDelayed(this::checkUserStatus, SPLASH_DELAY);
    }

    private void checkUserStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            navigateTo(LandingActivity.class);
            return;
        }

        // A user is signed in, check their Firestore document
        db.collection("users").document(currentUser.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot doc = task.getResult();
                        Student student = doc.toObject(Student.class);

                        if (student == null) { // Safety check
                            navigateTo(LandingActivity.class);
                            return;
                        }

                        // --- CHECKS ARE NOW IN THE CORRECT ORDER ---

                        // 1. Check for DELETION REQUEST (Highest Priority)
                        if (student.isDeletionRequested()) {
                            String message = "Your account is pending deletion. To cancel this request, please email alaokhemberly@gmail.com.";
                            navigateToSuspendedScreen(message); // This logs them out and shows the banned page
                            return;
                        }

                        // 2. Check for BANS
                        if (student.isBanned()) {
                            String dateString = "permanently";
                            if (student.getDeletionDate() > 0) {
                                dateString = "until " + formatDate(student.getDeletionDate());
                            }
                            String message = "Your account is banned " + dateString + ". It will be deleted after this date.";
                            navigateToSuspendedScreen(message);
                            return;
                        }

                        // 3. Check for SUSPENSIONS
                        if (student.isSuspended()) {
                            long now = System.currentTimeMillis();
                            if (now < student.getSuspendEndDate()) {
                                // Suspension is still active
                                String dateString = "for 1 week.";
                                if (student.getSuspendEndDate() > 0) {
                                    dateString = "until " + formatDate(student.getSuspendEndDate());
                                }
                                String message = "Your account is suspended " + dateString;
                                navigateToSuspendedScreen(message);
                                return;
                            } else {
                                // Suspension is over! Auto-unsuspend them.
                                Log.d("SplashActivity", "Suspension ended. Re-activating user.");
                                doc.getReference().update("isSuspended", false, "suspendEndDate", 0);
                                // Continue to the next check
                            }
                        }

                        // 4. Check for Verification (Last)
                        if (student.isVerified()) {
                            // User is signed in, verified, and not banned/suspended
                            navigateTo(StudentHomeActivity.class);
                        } else {
                            // User is signed in but NOT verified
                            navigateTo(PendingVerificationActivity.class);
                        }

                    } else {
                        // User is in Auth but not Firestore (admin deleted doc)
                        Toast.makeText(this, "Your account no longer exists.", Toast.LENGTH_SHORT).show();
                        navigateTo(LandingActivity.class);
                    }
                });
    }

    private void navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(SplashActivity.this, activityClass);
        startActivity(intent);
        finish(); // Close the splash activity
    }

    private String formatDate(long timestamp) {
        if (timestamp == 0) return "an unknown date";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private void navigateToSuspendedScreen(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        mAuth.signOut(); // Log them out

        Intent intent = new Intent(SplashActivity.this, AccountSuspendedActivity.class);
        intent.putExtra("STATUS_MESSAGE", message);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}