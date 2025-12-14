package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class PendingVerificationActivity extends AppCompatActivity {

    private Button buttonBackToHome;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration userListener;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_verification);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            navigateToLogin();
            return;
        }
        currentUserId = currentUser.getUid();

        buttonBackToHome = findViewById(R.id.buttonBackToHome);
        ImageView clockIcon = findViewById(R.id.clockIcon);

        Animation rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_clock);
        if (clockIcon != null) clockIcon.startAnimation(rotateAnimation);

        buttonBackToHome.setOnClickListener(v -> {
            mAuth.signOut();
            navigateToLogin();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        attachVerificationListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (userListener != null) userListener.remove();
    }

    private void attachVerificationListener() {
        if (currentUserId == null) return;

        DocumentReference userDoc = db.collection("users").document(currentUserId);
        userListener = userDoc.addSnapshotListener(this, (snapshot, e) -> {
            if (e != null) return;

            if (snapshot != null && snapshot.exists()) {
                Student student = snapshot.toObject(Student.class);
                if (student != null && student.isVerified()) {
                    if (userListener != null) userListener.remove();
                    showVerificationSuccessDialog();
                }
            }
        });
    }

    private void showVerificationSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Account Verified!")
                .setMessage("Your account has been approved. You will now be logged out. Please log in again to continue.")
                .setPositiveButton("OK", (dialog, which) -> {
                    mAuth.signOut();
                    navigateToLogin();
                })
                .setCancelable(false)
                .show();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(PendingVerificationActivity.this, StudentLoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}