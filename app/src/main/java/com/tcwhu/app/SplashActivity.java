package com.tcwhu.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1500;
    private static final int PERMISSION_REQUEST_CODE = 10;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        List<String> requiredPermissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.RECORD_AUDIO);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!requiredPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, requiredPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            startAppFlow();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            startAppFlow();
        }
    }

    private void startAppFlow() {
        new Handler(Looper.getMainLooper()).postDelayed(this::checkUserStatus, SPLASH_DELAY);
    }

    private void checkUserStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            navigateTo(LandingActivity.class);
            return;
        }

        db.collection("users").document(currentUser.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot doc = task.getResult();
                        Student student = doc.toObject(Student.class);

                        if (student == null) {
                            navigateTo(LandingActivity.class);
                            return;
                        }

                        if (student.isDeletionRequested()) {
                            navigateToSuspendedScreen("Account pending deletion. Contact support to cancel.");
                            return;
                        }

                        if (student.isBanned()) {
                            String msg = "Account banned permanently.";
                            if (student.getDeletionDate() > 0) msg += " Deletion on: " + formatDate(student.getDeletionDate());
                            navigateToSuspendedScreen(msg);
                            return;
                        }

                        if (student.isSuspended()) {
                            if (System.currentTimeMillis() < student.getSuspendEndDate()) {
                                navigateToSuspendedScreen("Account suspended until " + formatDate(student.getSuspendEndDate()));
                                return;
                            } else {
                                doc.getReference().update("isSuspended", false, "suspendEndDate", 0);
                            }
                        }

                        if (student.isVerified()) {
                            navigateTo(StudentHomeActivity.class);
                        } else {
                            navigateTo(PendingVerificationActivity.class);
                        }

                    } else {
                        Toast.makeText(this, "Account not found.", Toast.LENGTH_SHORT).show();
                        navigateTo(LandingActivity.class);
                    }
                });
    }

    private void navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(SplashActivity.this, activityClass);
        startActivity(intent);
        finish();
    }

    private String formatDate(long timestamp) {
        if (timestamp == 0) return "unknown date";
        return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(new Date(timestamp));
    }

    private void navigateToSuspendedScreen(String message) {
        mAuth.signOut();
        Intent intent = new Intent(SplashActivity.this, AccountSuspendedActivity.class);
        intent.putExtra("STATUS_MESSAGE", message);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}