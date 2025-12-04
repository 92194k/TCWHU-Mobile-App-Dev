package com.tcwhu.app;

import android.content.Intent;
import android.graphics.Color; // ADDED
import android.os.Bundle;
import android.text.Editable; // ADDED
import android.text.TextWatcher; // ADDED
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
import java.util.regex.Pattern;
import androidx.core.content.ContextCompat;


public class StudentSignUpActivity extends AppCompatActivity {

    private TextInputEditText inputStudentNumber, inputEmail, inputPassword, inputConfirmPassword, inputNickname, inputInterests;
    private AutoCompleteTextView inputYearLevel;
    private TextView textPasswordPolicyFeedback; // ADDED
    private MaterialCheckBox checkboxPrivacy;
    private Button buttonContinue;
    private FirebaseAuth mAuth;

    // --- CRITICAL FIX: Password Policy Constants ---
    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[@#$%^&+=!].*");
    // ------------------------------------------


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
        textPasswordPolicyFeedback = findViewById(R.id.textPasswordPolicyFeedback); // INITIALIZE

        setupToolbar();
        setupYearLevelDropdown();
        setupPasswordPolicyFeedback(); // NEW METHOD CALL

        // Handle clickable Privacy Policy link
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

    /**
     * Initializes listener to provide real-time feedback on password policy.
     */
    private void setupPasswordPolicyFeedback() {
        // Set up TextWatcher for real-time feedback
        inputPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Call validation to update the TextView color/text
                isPasswordPolicyMet(s.toString());
            }
        });
    }

    private boolean isPasswordPolicyMet(String password) {
        // Check all criteria individually
        boolean isLengthValid = password.length() >= MIN_LENGTH;
        boolean hasUppercase = UPPERCASE_PATTERN.matcher(password).matches();
        boolean hasLowercase = LOWERCASE_PATTERN.matcher(password).matches();
        boolean hasNumeric = NUMERIC_PATTERN.matcher(password).matches();
        boolean hasSpecialChar = SPECIAL_CHAR_PATTERN.matcher(password).matches();

        boolean allMet = isLengthValid && hasUppercase && hasLowercase && hasNumeric && hasSpecialChar;

        // Build detailed feedback message
        StringBuilder feedback = new StringBuilder();
        feedback.append("Password Requirements:\n");
        feedback.append(checkCriteria(isLengthValid, MIN_LENGTH + "+ Characters\n"));
        feedback.append(checkCriteria(hasUppercase, "1 Uppercase letter\n"));
        feedback.append(checkCriteria(hasLowercase, "1 Lowercase letter\n"));
        feedback.append(checkCriteria(hasNumeric, "1 Number\n"));
        feedback.append(checkCriteria(hasSpecialChar, "1 Special character (@#$%^&+=!)"));

        // Set the text and color feedback
        textPasswordPolicyFeedback.setText(feedback.toString());

        if (password.isEmpty()) {
            textPasswordPolicyFeedback.setText("Policy: 8+ chars, UC, LC, Number, Special Char.");
            textPasswordPolicyFeedback.setTextColor(ContextCompat.getColor(this, R.color.text_secondary)); // Gray
        } else if (allMet) {
            textPasswordPolicyFeedback.setTextColor(Color.parseColor("#388E3C")); // Green for success
        } else {
            textPasswordPolicyFeedback.setTextColor(ContextCompat.getColor(this, R.color.warning_text)); // Red for failure
        }

        return allMet;
    }

    /**
     * Helper to conditionally format the criteria text.
     */
    private String checkCriteria(boolean met, String requirement) {
        String status = met ? "✅ " : "❌ ";
        return status + requirement;
    }


    private void registerUserAndProceedToPhotoUpload() {
        String studentNumber = inputStudentNumber.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        String actualEmail = inputEmail.getText().toString().trim();

        mAuth.createUserWithEmailAndPassword(actualEmail, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            Map<String, Object> userData = new HashMap<>();

                            userData.put("studentNumber", studentNumber);
                            userData.put("email", actualEmail);
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
                        Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
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
        boolean isValid = true;

        // 1. Basic Field Checks
        if (studentNumber.isEmpty()) { inputStudentNumber.setError("Required"); isValid = false; } else inputStudentNumber.setError(null);
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError("Valid email required"); isValid = false;
        } else inputEmail.setError(null);
        if (nickname.isEmpty()) { inputNickname.setError("Required"); isValid = false; } else inputNickname.setError(null);
        if (yearLevel.isEmpty()) { inputYearLevel.setError("Required"); isValid = false; } else inputYearLevel.setError(null);

        // 2. Password Match Check
        if (!password.equals(confirmPassword)) {
            inputConfirmPassword.setError("Passwords do not match");
            isValid = false;
        } else inputConfirmPassword.setError(null);


        // 3. Password Policy Check (CRITICAL FIX)
        if (!isPasswordPolicyMet(password)) {
            // The policy feedback TextView already shows the detailed error
            inputPassword.setError("Password does not meet policy requirements.");
            isValid = false;
        } else {
            inputPassword.setError(null);
        }

        // 4. Privacy Check
        if (!checkboxPrivacy.isChecked()) {
            Toast.makeText(this, "You must agree to the privacy rules", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (!isValid) {
            Toast.makeText(this, "Please correct the errors before proceeding.", Toast.LENGTH_LONG).show();
        }

        return isValid;
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