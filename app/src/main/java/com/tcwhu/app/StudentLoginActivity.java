package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

public class StudentLoginActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputEditText inputStudentNumber;
    private TextInputEditText inputPassword;
    private Button buttonLogin;
    private Button buttonForgotPassword;
    private TextView textSignUp;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Find views and set up UI
        toolbar = findViewById(R.id.toolbar);
        inputStudentNumber = findViewById(R.id.inputStudentNumber);
        inputPassword = findViewById(R.id.inputPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonForgotPassword = findViewById(R.id.buttonForgotPassword);
        textSignUp = findViewById(R.id.textSignUp);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        buttonLogin.setOnClickListener(v -> handleLogin());
        buttonForgotPassword.setOnClickListener(v -> handleForgotPassword());
        textSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(StudentLoginActivity.this, StudentSignUpActivity.class);
            startActivity(intent);
        });
    }

    private void handleLogin() {
        String studentNumber = inputStudentNumber.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        if (studentNumber.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = studentNumber + "@tcwhu.app";

        // Step 1: Sign in with email and password
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Step 2: If sign-in is successful, check verification status in the database
                            checkUserVerificationStatus(user.getUid());
                        }
                    } else {
                        Toast.makeText(StudentLoginActivity.this, "Authentication failed. Please check your credentials.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserVerificationStatus(String userId) {
        // Get the user's document from the "users" collection in Firestore
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Boolean isVerified = document.getBoolean("isVerified");
                            if (isVerified != null && isVerified) {
                                // User IS verified -> Go to the main dashboard
                                navigateTo(StudentHomeActivity.class);
                            } else {
                                // User is NOT verified -> Go to the pending screen
                                navigateTo(PendingVerificationActivity.class);
                            }
                        } else {
                            // User exists in Auth but has no profile data, send to pending as a safe default
                            navigateTo(PendingVerificationActivity.class);
                        }
                    } else {
                        Toast.makeText(this, "Error checking user status.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(StudentLoginActivity.this, activityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleForgotPassword() {
        String studentNumber = inputStudentNumber.getText().toString().trim();
        if (studentNumber.isEmpty()) {
            inputStudentNumber.setError("Please enter your student number first");
            return;
        }
        String email = studentNumber + "@tcwhu.app";
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showForgotDialog();
                    } else {
                        Toast.makeText(StudentLoginActivity.this, "If a matching account exists, a password reset link has been sent.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showForgotDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Password Reset Email Sent")
                .setMessage("A link to reset your password has been sent to the email associated with your account by the admin.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }
}