package com.tcwhu.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import androidx.core.content.ContextCompat;

public class StudentSignUpActivity extends AppCompatActivity {

    private static final String TAG = "StudentSignUpActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextInputEditText inputStudentNumber, inputEmail, inputPassword, inputConfirmPassword, inputNickname, inputInterests;
    private AutoCompleteTextView inputYearLevel;
    private TextView textPasswordPolicyFeedback;
    private MaterialCheckBox checkboxPrivacy;
    private Button buttonContinue;

    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[@#$%^&+=!].*");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        inputStudentNumber = findViewById(R.id.inputStudentNumber);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        inputNickname = findViewById(R.id.inputNickname);
        inputYearLevel = findViewById(R.id.inputYearLevel);
        inputInterests = findViewById(R.id.inputInterests);
        checkboxPrivacy = findViewById(R.id.checkboxPrivacy);
        buttonContinue = findViewById(R.id.buttonContinue);
        textPasswordPolicyFeedback = findViewById(R.id.textPasswordPolicyFeedback);

        setupToolbar();
        setupYearLevelDropdown();
        setupPasswordPolicyFeedback();

        findViewById(R.id.textPrivacyPolicyLinkClickable).setOnClickListener(v -> startActivity(new Intent(this, PrivacyPolicyActivity.class)));
        buttonContinue.setOnClickListener(v -> attemptSignUp());
    }

    private void attemptSignUp() {
        if (!isFormValid()) return;

        final String studentNumber = inputStudentNumber.getText().toString().trim();
        final String email = inputEmail.getText().toString().trim();
        final String password = inputPassword.getText().toString().trim();
        final String nickname = inputNickname.getText().toString().trim();
        final String yearLevel = inputYearLevel.getText().toString().trim();
        final String interests = inputInterests.getText().toString().trim();

        buttonContinue.setEnabled(false);
        buttonContinue.setText("Checking Student Number...");

        checkStudentNumberAvailability(email, password, studentNumber, nickname, yearLevel, interests);
    }

    private void checkStudentNumberAvailability(String email, String password, String studentNumber, String nickname, String yearLevel, String interests) {
        db.collection("users")
                .whereEqualTo("studentNumber", studentNumber)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            registerUser(email, password, studentNumber, nickname, yearLevel, interests);
                        } else {
                            showStudentNumberConflictDialog();
                            resetButton();
                        }
                    } else {
                        Toast.makeText(this, "Error checking student number.", Toast.LENGTH_LONG).show();
                        resetButton();
                    }
                });
    }

    private void resetButton() {
        buttonContinue.setEnabled(true);
        buttonContinue.setText(R.string.continue_to_upload_photos);
    }

    private void showStudentNumberConflictDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Account Already Exists")
                .setMessage("A user account with this Student Number already exists.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void registerUser(String email, String password, String studentNumber, String nickname, String yearLevel, String interests) {
        buttonContinue.setText("Creating Account...");
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            saveUserProfile(firebaseUser.getUid(), email, studentNumber, nickname, yearLevel, interests);
                        }
                    } else {
                        Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        resetButton();
                    }
                });
    }

    private void saveUserProfile(String userId, String email, String studentNumber, String nickname, String yearLevel, String interests) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("studentNumber", studentNumber);
        userData.put("email", email);
        userData.put("nickname", nickname);
        userData.put("yearLevel", yearLevel);
        userData.put("interests", interests);
        userData.put("role", "student");
        userData.put("isVerified", false);
        userData.put("isBanned", false);
        userData.put("isSuspended", false);
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("avatar", "🧑‍🎓");

        db.collection("users").document(userId).set(userData)
                .addOnSuccessListener(aVoid -> {
                    Intent intent = new Intent(this, PhotoUploadActivity.class);
                    intent.putExtra("userId", userId);
                    intent.putExtra("userData", (Serializable) userData);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Profile creation failed.", Toast.LENGTH_LONG).show();
                    if (mAuth.getCurrentUser() != null) mAuth.getCurrentUser().delete();
                    resetButton();
                });
    }

    private void setupPasswordPolicyFeedback() {
        inputPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { isPasswordPolicyMet(s.toString()); }
        });
    }

    private boolean isPasswordPolicyMet(String password) {
        boolean isLengthValid = password.length() >= MIN_LENGTH;
        boolean hasUppercase = UPPERCASE_PATTERN.matcher(password).matches();
        boolean hasLowercase = LOWERCASE_PATTERN.matcher(password).matches();
        boolean hasNumeric = NUMERIC_PATTERN.matcher(password).matches();
        boolean hasSpecialChar = SPECIAL_CHAR_PATTERN.matcher(password).matches();
        boolean allMet = isLengthValid && hasUppercase && hasLowercase && hasNumeric && hasSpecialChar;

        StringBuilder feedback = new StringBuilder("Password Requirements:\n");
        feedback.append(checkCriteria(isLengthValid, MIN_LENGTH + "+ Characters\n"));
        feedback.append(checkCriteria(hasUppercase, "1 Uppercase letter\n"));
        feedback.append(checkCriteria(hasLowercase, "1 Lowercase letter\n"));
        feedback.append(checkCriteria(hasNumeric, "1 Number\n"));
        feedback.append(checkCriteria(hasSpecialChar, "1 Special character (@#$%^&+=!)"));

        textPasswordPolicyFeedback.setText(feedback.toString());

        if (password.isEmpty()) {
            textPasswordPolicyFeedback.setText("Policy: 8+ chars, UC, LC, Number, Special Char.");
            textPasswordPolicyFeedback.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        } else if (allMet) {
            textPasswordPolicyFeedback.setTextColor(Color.parseColor("#388E3C"));
        } else {
            textPasswordPolicyFeedback.setTextColor(ContextCompat.getColor(this, R.color.warning_text));
        }
        return allMet;
    }

    private String checkCriteria(boolean met, String requirement) {
        return (met ? "✅ " : "❌ ") + requirement;
    }

    private boolean isFormValid() {
        String studentNumber = inputStudentNumber.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String confirmPassword = inputConfirmPassword.getText().toString().trim();
        String nickname = inputNickname.getText().toString().trim();
        String yearLevel = inputYearLevel.getText().toString().trim();
        boolean isValid = true;

        if (studentNumber.isEmpty()) { inputStudentNumber.setError("Required"); isValid = false; }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) { inputEmail.setError("Valid email required"); isValid = false; }
        if (nickname.isEmpty()) { inputNickname.setError("Required"); isValid = false; }
        if (yearLevel.isEmpty()) { inputYearLevel.setError("Required"); isValid = false; }

        if (!password.equals(confirmPassword)) {
            inputConfirmPassword.setError("Passwords do not match");
            isValid = false;
        }

        if (!isPasswordPolicyMet(password)) {
            inputPassword.setError("Password does not meet policy requirements.");
            isValid = false;
        }

        if (!checkboxPrivacy.isChecked()) {
            Toast.makeText(this, "You must agree to the privacy rules", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupYearLevelDropdown() {
        String[] yearLevels = new String[]{"1st Year", "2nd Year", "3rd Year", "4th Year"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, yearLevels);
        inputYearLevel.setAdapter(adapter);
    }
}