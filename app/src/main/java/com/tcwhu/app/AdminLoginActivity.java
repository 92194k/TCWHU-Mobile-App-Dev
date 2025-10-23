package com.tcwhu.app;

import android.content.Intent; // Import the Intent class
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

public class AdminLoginActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputEditText inputAdminUsername;
    private TextInputEditText inputAccessCode;
    private Button buttonAdminLogin;
    private Button buttonBackToStudent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

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

        buttonBackToStudent.setOnClickListener(v -> {
            // Simply finish this activity to go back to the Landing screen
            finish();
        });
    }

    private void handleAdminLogin() {
        String username = inputAdminUsername.getText().toString().trim();
        String accessCode = inputAccessCode.getText().toString().trim();

        if (username.isEmpty() || accessCode.isEmpty()) {
            Toast.makeText(this, "Please enter username and access code", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- THIS IS THE UPDATED PART --- ✅
        // For now, we'll use a simple hardcoded check.
        // A real app would verify this securely against a database.
        if (username.equals("admin") && accessCode.equals("password123")) {
            // If login is successful, navigate to the Admin Dashboard
            Intent intent = new Intent(this, AdminDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Close this activity
        } else {
            // If login fails, show an error message
            Toast.makeText(this, "Invalid admin credentials", Toast.LENGTH_SHORT).show();
        }
    }
}