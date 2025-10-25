package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore; // <-- MISSING
import com.google.firebase.firestore.Query; // <-- MISSING

public class AdminLoginActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputEditText inputAdminUsername;
    private TextInputEditText inputAccessCode;
    private Button buttonAdminLogin;
    private Button buttonBackToStudent;

    private FirebaseFirestore db; // <-- MISSING
    private static final String ACCESS_CODE_DOC_ID = "GLOBAL_ACCESS_CODE"; // <-- MISSING

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        db = FirebaseFirestore.getInstance(); // <-- MISSING

        // Find views
        toolbar = findViewById(R.id.toolbar);
        inputAdminUsername = findViewById(R.id.inputAdminUsername);
        inputAccessCode = findViewById(R.id.inputAccessCode);
        buttonAdminLogin = findViewById(R.id.buttonAdminLogin);
        buttonBackToStudent = findViewById(R.id.buttonBackToStudent);

        // Setup Toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Setup button clicks
        buttonAdminLogin.setOnClickListener(v -> handleAdminLogin());
        buttonBackToStudent.setOnClickListener(v -> finish());
    }

    private void handleAdminLogin() {
        String username = inputAdminUsername.getText().toString().trim();
        String accessCode = inputAccessCode.getText().toString().trim();

        if (username.isEmpty() || accessCode.isEmpty()) {
            Toast.makeText(this, "Please enter username and access code", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Check the global access code (FROM FIRESTORE)
        db.collection("settings").document(ACCESS_CODE_DOC_ID).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && accessCode.equals(doc.getString("code"))) {
                        // Global access code is correct, now check if the username is a registered admin
                        checkAdminUsername(username);
                    } else {
                        Toast.makeText(this, "Invalid access code.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Admin access check failed.", Toast.LENGTH_SHORT).show();
                });
    }

    // <-- MISSING CHECK ADMIN USERNAME METHOD -->
    private void checkAdminUsername(String username) {
        // 2. Query the 'admins' collection for the matching username (email)
        db.collection("admins").whereEqualTo("email", username).limit(1).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Username (email) found in the admins collection
                        Toast.makeText(this, "Admin Login Successful!", Toast.LENGTH_SHORT).show();

                        // Navigate to Admin Dashboard
                        Intent intent = new Intent(this, AdminDashboardActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Admin user not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Admin user check failed.", Toast.LENGTH_SHORT).show();
                });
    }
}