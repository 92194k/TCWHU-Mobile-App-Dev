package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StudentLoginActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputEditText inputStudentNumber, inputPassword;
    private Button buttonLogin, buttonForgotPassword;
    private TextView textSignUp;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        toolbar = findViewById(R.id.toolbar);
        inputStudentNumber = findViewById(R.id.inputStudentNumber);
        inputPassword = findViewById(R.id.inputPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonForgotPassword = findViewById(R.id.buttonForgotPassword);
        textSignUp = findViewById(R.id.textSignUp);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        buttonLogin.setOnClickListener(v -> handleLogin());
        buttonForgotPassword.setOnClickListener(v -> handleForgotPassword());
        textSignUp.setOnClickListener(v -> startActivity(new Intent(this, StudentSignUpActivity.class)));
    }

    private void handleLogin() {
        String studentNumber = inputStudentNumber.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        if (studentNumber.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = studentNumber + "@tcwhu.app";

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserStatusAndNavigate(user.getUid());
                        }
                    } else {
                        Toast.makeText(this, "Authentication failed. Please check your credentials.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * This method now checks all user statuses in the correct order:
     * 1. Deletion Requested?
     * 2. Banned?
     * 3. Suspended?
     * 4. Verified?
     */
    private void checkUserStatusAndNavigate(String userId) {
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot doc = task.getResult();
                        Student student = doc.toObject(Student.class);

                        if (student == null) {
                            Toast.makeText(this, "Could not find user record.", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                            return;
                        }

                        // --- NEW CHECK (HIGHEST PRIORITY) ---
                        // 1. Check for DELETION REQUEST
                        if (student.isDeletionRequested()) {
                            String message = "Your account is pending deletion. To cancel this request, please email alaokhemberly@gmail.com.";
                            navigateToSuspendedScreen(message); // This logs them out and shows the banned page
                            return;
                        }
                        // --- END OF NEW CHECK ---

                        // 2. Check for BANS
                        if (student.isBanned()) {
                            String dateString = (student.getDeletionDate() > 0) ? "until " + formatDate(student.getDeletionDate()) : "permanently";
                            String message = "Your account is banned " + dateString + ". It will be deleted after this date.";
                            navigateToSuspendedScreen(message);
                            return;
                        }

                        // 3. Check for SUSPENSIONS
                        if (student.isSuspended()) {
                            long now = System.currentTimeMillis();
                            if (now < student.getSuspendEndDate()) {
                                // Suspension is active
                                String dateString = (student.getSuspendEndDate() > 0) ? "until " + formatDate(student.getSuspendEndDate()) : "for 1 week.";
                                String message = "Your account is suspended " + dateString;
                                navigateToSuspendedScreen(message);
                                return;
                            } else {
                                // Suspension is over! Auto-unsuspend them.
                                Log.d("LoginActivity", "Suspension ended. Re-activating user.");
                                doc.getReference().update("isSuspended", false, "suspendEndDate", 0);
                                // Fall through to the next check...
                            }
                        }

                        // 4. User is Active: Check for Verification
                        if (student.isVerified()) {
                            // User is "active" AND "verified" -> Proceed with login
                            FirebaseMessaging.getInstance().subscribeToTopic("all_users");
                            saveFCMToken(userId);
                            navigateTo(StudentHomeActivity.class);
                        } else {
                            // User is "active" but NOT "verified"
                            navigateTo(PendingVerificationActivity.class);
                        }

                    } else {
                        // User is in Auth but not Firestore
                        Toast.makeText(this, "User data not found. Please contact admin.", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                    }
                });
    }


    private void saveFCMToken(String userId) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("MyToken", "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    Log.d("MyToken", "My FCM token is: " + token);

                    db.collection("users")
                            .document(userId)
                            .update("notificationToken", token)
                            .addOnSuccessListener(a -> Log.d("FCM", "Token successfully saved to Firestore"))
                            .addOnFailureListener(e -> Log.e("FCM", "Error saving token to Firestore", e));
                });
    }

    private void navigateTo(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToSuspendedScreen(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        mAuth.signOut(); // Log them out

        Intent intent = new Intent(StudentLoginActivity.this, AccountSuspendedActivity.class);
        intent.putExtra("STATUS_MESSAGE", message); // Pass the custom message
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String formatDate(long timestamp) {
        if (timestamp == 0) return "an unknown date";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
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
                        Toast.makeText(this, "If a matching account exists, a password reset link has been sent.", Toast.LENGTH_LONG).show();
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