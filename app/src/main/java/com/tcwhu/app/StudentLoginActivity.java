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
import com.google.firebase.firestore.Query; // IMPORT ADDED for lookup
import com.google.firebase.firestore.QuerySnapshot; // IMPORT ADDED for lookup
import com.google.firebase.messaging.FirebaseMessaging; // IMPORT ADDED for token

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

        // --- CRITICAL CHANGE: Use Firestore to find the actual email for login ---
        db.collection("users")
                .whereEqualTo("studentNumber", studentNumber)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String email = querySnapshot.getDocuments().get(0).getString("email");
                        if (email != null) {
                            attemptFirebaseLogin(email, password);
                        } else {
                            Toast.makeText(StudentLoginActivity.this, "Error: User email not found in database.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(StudentLoginActivity.this, "Authentication failed. Please check your Student Number.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(StudentLoginActivity.this, "Login error. Check connection.", Toast.LENGTH_LONG).show();
                });
    }

    private void attemptFirebaseLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserStatusAndNavigate(user.getUid());
                        }
                    } else {
                        // This handles incorrect password/generic auth errors for the real email
                        Toast.makeText(StudentLoginActivity.this, "Authentication failed. Incorrect password.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * CRITICAL LOGIC FOR FORGOT PASSWORD:
     * 1. Takes the Student Number.
     * 2. LOOKS UP the actual email address in Firestore.
     * 3. Calls the Firebase method to send the reset link to that looked-up email.
     */
    private void handleForgotPassword() {
        String studentNumber = inputStudentNumber.getText().toString().trim();

        if (studentNumber.isEmpty()) {
            inputStudentNumber.setError("Please enter your student number to reset your password.");
            return;
        }

        // 1. Query Firestore for the user's actual email address
        db.collection("users")
                .whereEqualTo("studentNumber", studentNumber)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String userEmail = querySnapshot.getDocuments().get(0).getString("email");

                        if (userEmail != null) {
                            // 2. Send reset email using the actual email found
                            mAuth.sendPasswordResetEmail(userEmail)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            showForgotDialog(userEmail);
                                        } else {
                                            // Security Vague Message: If Firebase rejects the email (e.g., deleted), show this.
                                            Toast.makeText(StudentLoginActivity.this,
                                                    "If the account exists, a password reset link has been sent.",
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(StudentLoginActivity.this,
                                    "Account not found.",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // Display a generic message for security purposes
                        Toast.makeText(StudentLoginActivity.this,
                                "If the account exists, a password reset link has been sent.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(StudentLoginActivity.this,
                            "Error checking database for account.",
                            Toast.LENGTH_LONG).show();
                });
    }

    private void showForgotDialog(String email) {
        new AlertDialog.Builder(this)
                .setTitle("Password Reset Email Sent")
                .setMessage("A link to reset your password has been sent to the following email address: " + email + ". Please check your inbox.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // NOTE: saveFCMToken method has been removed from this file as it was incomplete
    // and would better belong with the successful login or other appropriate lifecycle event.

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

                        if (student.isDeletionRequested()) {
                            String message = "Your account is pending deletion. To cancel this request, please email alaokhemberly@gmail.com.";
                            navigateToSuspendedScreen(message);
                            return;
                        }

                        if (student.isBanned()) {
                            String dateString = (student.getDeletionDate() > 0) ? "until " + formatDate(student.getDeletionDate()) : "permanently";
                            String message = "Your account is banned " + dateString + ". It will be deleted after this date.";
                            navigateToSuspendedScreen(message);
                            return;
                        }

                        if (student.isSuspended()) {
                            long now = System.currentTimeMillis();
                            if (now < student.getSuspendEndDate()) {
                                String dateString = (student.getSuspendEndDate() > 0) ? "until " + formatDate(student.getSuspendEndDate()) : "for 1 week.";
                                String message = "Your account is suspended " + dateString;
                                navigateToSuspendedScreen(message);
                                return;
                            } else {
                                Log.d("LoginActivity", "Suspension ended. Re-activating user.");
                                doc.getReference().update("isSuspended", false, "suspendEndDate", 0);
                            }
                        }

                        if (student.isVerified()) {
                            FirebaseMessaging.getInstance().subscribeToTopic("all_users");
                            // saveFCMToken(userId); // Re-introduce or fix this call if needed
                            navigateTo(StudentHomeActivity.class);
                        } else {
                            navigateTo(PendingVerificationActivity.class);
                        }

                    } else {
                        Toast.makeText(this, "User data not found. Please contact admin.", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                    }
                });
    }

    private void navigateTo(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToSuspendedScreen(String message) {
        mAuth.signOut();
        Intent intent = new Intent(StudentLoginActivity.this, AccountSuspendedActivity.class);
        intent.putExtra("STATUS_MESSAGE", message);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String formatDate(long timestamp) {
        if (timestamp == 0) return "an unknown date";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}