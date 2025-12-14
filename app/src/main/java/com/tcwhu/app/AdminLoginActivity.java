package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminLoginActivity extends AppCompatActivity {

    private TextInputEditText inputAdminUsername;
    private TextInputEditText inputAccessCode;
    private FirebaseFirestore db;
    private static final String ACCESS_CODE_DOC_ID = "GLOBAL_ACCESS_CODE";
    private static final String MASTER_KEY = "9999";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        db = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        inputAdminUsername = findViewById(R.id.inputAdminUsername);
        inputAccessCode = findViewById(R.id.inputAccessCode);
        Button buttonAdminLogin = findViewById(R.id.buttonAdminLogin);
        Button buttonBackToStudent = findViewById(R.id.buttonBackToStudent);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        buttonAdminLogin.setOnClickListener(v -> handleAdminLogin());
        buttonBackToStudent.setOnClickListener(v -> finish());
    }

    private void handleAdminLogin() {
        String username = inputAdminUsername.getText().toString().trim();
        String accessCode = inputAccessCode.getText().toString().trim();

        if (username.isEmpty() || accessCode.isEmpty()) {
            Toast.makeText(this, "Please enter your username and access code.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Developer/Demo bypass
        if (accessCode.equals(MASTER_KEY)) {
            navigateToDashboard();
            return;
        }

        db.collection("settings").document(ACCESS_CODE_DOC_ID).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && accessCode.equals(doc.getString("code"))) {
                        checkAdminUsername(username);
                    } else {
                        Toast.makeText(this, "Incorrect username or password.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Connection failed.", Toast.LENGTH_SHORT).show());
    }

    private void checkAdminUsername(String username) {
        db.collection("admins").whereEqualTo("email", username).limit(1).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        navigateToDashboard();
                    } else {
                        Toast.makeText(this, "Admin user not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Verification failed.", Toast.LENGTH_SHORT).show());
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, AdminDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}