package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class StudentSignUpActivity extends AppCompatActivity {

    private TextInputEditText inputStudentNumber, inputEmail, inputPassword, inputConfirmPassword, inputNickname, inputInterests;
    private AutoCompleteTextView inputYearLevel;
    private MaterialCheckBox checkboxPrivacy;
    private Button buttonContinue;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_signup);

        mAuth = FirebaseAuth.getInstance();

        inputStudentNumber = findViewById(R.id.inputStudentNumber);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        inputNickname = findViewById(R.id.inputNickname);
        inputYearLevel = findViewById(R.id.inputYearLevel);
        inputInterests = findViewById(R.id.inputInterests);
        checkboxPrivacy = findViewById(R.id.checkboxPrivacy);
        buttonContinue = findViewById(R.id.buttonContinue);

        setupToolbar();
        setupYearLevelDropdown();

        // ✅ Handle clickable Privacy Policy link
        TextView textPrivacyPolicyLinkClickable = findViewById(R.id.textPrivacyPolicyLinkClickable);
        textPrivacyPolicyLinkClickable.setOnClickListener(v -> {
            Intent intent = new Intent(StudentSignUpActivity.this, PrivacyPolicyActivity.class);
            startActivity(intent);
        });

        // Continue button
        buttonContinue.setOnClickListener(v -> {
            if (isFormValid()) {
                registerUserAndProceedToPhotoUpload();
            }
        });
    }

    private void registerUserAndProceedToPhotoUpload() {
        String studentNumber = inputStudentNumber.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        // --- CRITICAL CHANGE 1: Use the user's REAL email for Firebase Auth ---
        String actualEmail = inputEmail.getText().toString().trim();

        mAuth.createUserWithEmailAndPassword(actualEmail, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            Map<String, Object> userData = new HashMap<>();

                            // --- CRITICAL CHANGE 2: Save the studentNumber separately in Firestore ---
                            userData.put("studentNumber", studentNumber);
                            userData.put("email", actualEmail); // Also save the real email

                            userData.put("nickname", inputNickname.getText().toString().trim());
                            userData.put("yearLevel", inputYearLevel.getText().toString().trim());
                            userData.put("interests", inputInterests.getText().toString().trim());
                            userData.put("isVerified", false);
                            userData.put("isBanned", false);
                            userData.put("isSuspended", false);
                            userData.put("createdAt", System.currentTimeMillis());

                            Intent intent = new Intent(this, PhotoUploadActivity.class);
                            intent.putExtra("userId", firebaseUser.getUid());
                            intent.putExtra("userData", (Serializable) userData);
                            startActivity(intent);
                        }
                    } else {
                        Toast.makeText(this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean isFormValid() {
        String studentNumber = inputStudentNumber.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String confirmPassword = inputConfirmPassword.getText().toString().trim();
        String nickname = inputNickname.getText().toString().trim();
        String yearLevel = inputYearLevel.getText().toString().trim();

        if (studentNumber.isEmpty() || email.isEmpty() || password.isEmpty() ||
                confirmPassword.isEmpty() || nickname.isEmpty() || yearLevel.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError("Please enter a valid email address");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            inputConfirmPassword.setError("Passwords do not match");
            return false;
        }
        if (!checkboxPrivacy.isChecked()) {
            Toast.makeText(this, "You must agree to the privacy rules", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupYearLevelDropdown() {
        String[] yearLevels = new String[]{"1st Year", "2nd Year", "3rd Year", "4th Year"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, yearLevels);
        inputYearLevel.setAdapter(adapter);
    }
}