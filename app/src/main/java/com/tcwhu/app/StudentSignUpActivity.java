package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore; // Import Firestore
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import java.util.HashMap; // Import HashMap
import java.util.Map;     // Import Map

public class StudentSignUpActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> avatarSelectorLauncher;
    private String selectedAvatar = null;

    private MaterialToolbar toolbar;
    private TextInputEditText inputStudentNumber, inputPassword, inputConfirmPassword, inputNickname, inputInterests;
    private AutoCompleteTextView inputYearLevel;
    private MaterialCheckBox checkboxPrivacy;
    private Button buttonContinue;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // Declare a Firestore instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_signup);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        avatarSelectorLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        selectedAvatar = result.getData().getStringExtra("selectedAvatar");
                        registerUser();
                    }
                }
        );

        // Find views and set up UI
        toolbar = findViewById(R.id.toolbar);
        inputStudentNumber = findViewById(R.id.inputStudentNumber);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        inputNickname = findViewById(R.id.inputNickname);
        inputYearLevel = findViewById(R.id.inputYearLevel);
        inputInterests = findViewById(R.id.inputInterests);
        checkboxPrivacy = findViewById(R.id.checkboxPrivacy);
        buttonContinue = findViewById(R.id.buttonContinue);
        setupToolbar();
        setupYearLevelDropdown();
        buttonContinue.setOnClickListener(v -> handleContinueClick());
    }

    private void registerUser() {
        String studentNumber = inputStudentNumber.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String email = studentNumber + "@tcwhu.app";

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // After creating the user, save their profile data to Firestore.
                            saveUserProfile(firebaseUser.getUid());
                        }
                    } else {
                        Toast.makeText(StudentSignUpActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserProfile(String userId) {
        // Get the rest of the form data
        String studentNumber = inputStudentNumber.getText().toString().trim();
        String nickname = inputNickname.getText().toString().trim();
        String yearLevel = inputYearLevel.getText().toString().trim();
        String interests = inputInterests.getText().toString().trim();

        // Create a new "user" object with all the data
        Map<String, Object> user = new HashMap<>();
        user.put("studentNumber", studentNumber);
        user.put("nickname", nickname);
        user.put("yearLevel", yearLevel);
        user.put("interests", interests);
        user.put("avatar", selectedAvatar);
        user.put("isVerified", false); // New users are not verified by default
        user.put("createdAt", System.currentTimeMillis());

        // Add a new document with the user's ID to the "users" collection
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    // Data saved successfully! Now navigate to the verification screen.
                    Intent intent = new Intent(StudentSignUpActivity.this, PendingVerificationActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // If saving data fails, show an error
                    Toast.makeText(StudentSignUpActivity.this, "Error saving profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void handleContinueClick() {
        if (isFormValid()) {
            Intent intent = new Intent(this, AvatarSelectorActivity.class);
            avatarSelectorLauncher.launch(intent);
        }
    }

    private boolean isFormValid() {
        String studentNumber = inputStudentNumber.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String confirmPassword = inputConfirmPassword.getText().toString().trim();
        String nickname = inputNickname.getText().toString().trim();
        String yearLevel = inputYearLevel.getText().toString().trim();

        if (studentNumber.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || nickname.isEmpty() || yearLevel.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
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
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupYearLevelDropdown() {
        String[] yearLevels = new String[]{"1st Year", "2nd Year", "3rd Year", "4th Year"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, yearLevels);
        inputYearLevel.setAdapter(adapter);
    }
}