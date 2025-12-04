package com.tcwhu.app;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class StudentProfileFragment extends Fragment {

    private TextView textAvatar, textNickname, textYearLevel, textInterests, textStudentNumber;
    private Button buttonLogout, buttonDeleteAccount, buttonChangeAvatar, buttonEditNickname, buttonEditInterests, buttonChangePassword;
    private SwitchMaterial switchTheme;
    private ImageView iconTheme;
    private TextView textThemeLabel;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private Student currentStudentProfile;

    private ActivityResultLauncher<Intent> avatarSelectorLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        avatarSelectorLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        String newAvatar = result.getData().getStringExtra("selectedAvatar");
                        if (newAvatar != null) {
                            updateAvatarInDatabase(newAvatar);
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_profile, container, false);

        // Find all your views
        textAvatar = view.findViewById(R.id.textAvatar);
        textNickname = view.findViewById(R.id.textNickname);
        textYearLevel = view.findViewById(R.id.textYearLevel);
        textInterests = view.findViewById(R.id.textInterests);
        textStudentNumber = view.findViewById(R.id.textStudentNumber);
        buttonLogout = view.findViewById(R.id.buttonLogout);
        buttonDeleteAccount = view.findViewById(R.id.buttonDeleteAccount);
        buttonChangeAvatar = view.findViewById(R.id.buttonChangeAvatar);
        buttonEditNickname = view.findViewById(R.id.buttonEditNickname);
        buttonEditInterests = view.findViewById(R.id.buttonEditInterests);
        buttonChangePassword = view.findViewById(R.id.buttonChangePassword);
        switchTheme = view.findViewById(R.id.switchTheme);
        iconTheme = view.findViewById(R.id.iconTheme);
        textThemeLabel = view.findViewById(R.id.textThemeLabel);

        loadUserProfile();
        setupThemeSwitch();

        buttonLogout.setOnClickListener(v -> logoutUser());

        buttonChangeAvatar.setOnClickListener(v -> launchAvatarSelector());
        buttonEditNickname.setOnClickListener(v -> {
            if (currentStudentProfile != null) showEditFieldDialog("nickname", currentStudentProfile.getNickname());
        });
        buttonEditInterests.setOnClickListener(v -> {
            if (currentStudentProfile != null) showEditFieldDialog("interests", currentStudentProfile.getInterests());
        });
        buttonChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        return view;
    }

    private void setupThemeSwitch() {
        if (getContext() == null) return;
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isCurrentlyDark = currentNightMode == Configuration.UI_MODE_NIGHT_YES;
        updateThemeUI(isCurrentlyDark);
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                ThemeSwitcher.setTheme(getContext(), ThemeSwitcher.THEME_DARK);
                updateThemeUI(true);
            } else {
                ThemeSwitcher.setTheme(getContext(), ThemeSwitcher.THEME_LIGHT);
                updateThemeUI(false);
            }
        });
    }

    private void updateThemeUI(boolean isDarkThemeEnabled) {
        if (isDarkThemeEnabled) {
            switchTheme.setChecked(true);
            textThemeLabel.setText("Light Mode");
            iconTheme.setImageResource(R.drawable.ic_sun);
        } else {
            switchTheme.setChecked(false);
            textThemeLabel.setText("Dark Mode");
            iconTheme.setImageResource(R.drawable.ic_dark_mode);
        }
    }

    private void launchAvatarSelector() {
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), AvatarSelectorActivity.class);
        intent.putExtra("userId", currentUser.getUid());
        intent.putExtra("fromProfile", true);
        avatarSelectorLauncher.launch(intent);
    }

    private void updateAvatarInDatabase(String newAvatar) {
        if (currentUser == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("avatar", newAvatar);
        db.collection("users").document(currentUser.getUid()).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Avatar updated successfully!", Toast.LENGTH_SHORT).show();
                    loadUserProfile();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update avatar.", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserProfile() {
        if (currentUser == null) return;
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentStudentProfile = documentSnapshot.toObject(Student.class);
                        if (currentStudentProfile != null) {
                            textAvatar.setText(currentStudentProfile.getAvatar());
                            textNickname.setText(currentStudentProfile.getNickname());
                            textYearLevel.setText(currentStudentProfile.getYearLevel());
                            textStudentNumber.setText(currentStudentProfile.getStudentNumber());
                            textInterests.setText(currentStudentProfile.getInterests());

                            // Setup the deletion button based on profile status
                            if (currentStudentProfile.isDeletionRequested()) {
                                buttonDeleteAccount.setEnabled(false);
                                buttonDeleteAccount.setText("Deletion Requested");
                            } else {
                                buttonDeleteAccount.setEnabled(true);
                                buttonDeleteAccount.setText("Request Account Deletion");
                                buttonDeleteAccount.setOnClickListener(v -> showStudentNumberConfirmationDialog());
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load profile.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showEditFieldDialog(String fieldName, String currentValue) {
        if (getContext() == null || currentStudentProfile == null) return;
        final String fieldKey = fieldName;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit " + fieldName);
        final EditText input = new EditText(getContext());
        input.setText(currentValue);
        builder.setView(input);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newValue = input.getText().toString().trim();
            if (!newValue.isEmpty()) {
                updateProfileField(fieldKey, newValue);
            } else {
                Toast.makeText(getContext(), fieldName + " cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateProfileField(String field, String value) {
        if (currentUser == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put(field, value);
        db.collection("users").document(currentUser.getUid()).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), field + " updated successfully.", Toast.LENGTH_SHORT).show();
                    loadUserProfile();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Enforces the new password policy and provides specific error messages.
     * Policy: 8+ chars, UC, LC, Number, Special Char.
     * @param password The new password string to validate.
     * @return A specific error message, or null if validation passes.
     */
    private String validatePassword(String password) {
        if (password.length() < 8) {
            return "Password must be at least 8 characters long.";
        }
        // Check for at least one uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter.";
        }
        // Check for at least one lowercase letter
        if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter.";
        }
        // Check for at least one number
        if (!password.matches(".*[0-9].*")) {
            return "Password must contain at least one number.";
        }
        // Check for at least one special character (not letter or digit)
        if (!password.matches(".*[^a-zA-Z0-9].*")) {
            return "Password must contain at least one special character.";
        }
        return null; // Password is valid
    }

    private void showChangePasswordDialog() {
        if (getContext() == null || currentUser == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, null);

        // Use TextInputEditText to match the XML layout
        final TextInputEditText inputCurrentPassword = dialogView.findViewById(R.id.inputCurrentPassword);
        final TextInputEditText inputNewPassword = dialogView.findViewById(R.id.inputNewPassword);
        final TextInputEditText inputConfirmPassword = dialogView.findViewById(R.id.inputConfirmPassword);

        final AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Update", null) // Set to null to override behavior below
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String current = inputCurrentPassword.getText().toString();
                String newPass = inputNewPassword.getText().toString();
                String confirmPass = inputConfirmPassword.getText().toString();

                // 1. Password Match Check
                if (!newPass.equals(confirmPass)) {
                    Toast.makeText(getContext(), "New passwords do not match.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 2. Password Policy Check (Provides specific error feedback)
                String validationError = validatePassword(newPass);
                if (validationError != null) {
                    Toast.makeText(getContext(), validationError, Toast.LENGTH_LONG).show();
                    return;
                }

                // If all checks pass, dismiss the dialog and proceed with reauthentication
                dialog.dismiss();
                reauthenticateAndChangePassword(current, newPass);
            });
        });

        dialog.show();
    }

    private void reauthenticateAndChangePassword(String currentPassword, String newPassword) {
        String email = currentUser.getEmail();
        if (email == null) {
            Toast.makeText(getContext(), "Cannot change password. User email not found.", Toast.LENGTH_LONG).show();
            return;
        }
        AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);
        currentUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    currentUser.updatePassword(newPassword)
                            .addOnSuccessListener(task -> {
                                Toast.makeText(getContext(), "Password updated successfully!", Toast.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed to update password: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Authentication failed. Current password incorrect.", Toast.LENGTH_LONG).show();
                });
    }

    // --- Step 1 of Deletion ---
    private void showStudentNumberConfirmationDialog() {
        if (getContext() == null || currentStudentProfile == null) {
            Toast.makeText(getContext(), "Profile data not loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Inflate the custom layout (dialog_request_deletion.xml)
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_request_deletion, null);
        final TextInputEditText inputNumber = dialogView.findViewById(R.id.inputConfirmStudentNumber);
        final TextInputEditText inputReason = dialogView.findViewById(R.id.inputDeletionReason);
        final TextInputLayout layoutNumber = dialogView.findViewById(R.id.layoutStudentNumber);
        final TextInputLayout layoutReason = dialogView.findViewById(R.id.layoutReason);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton("Submit Request", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String number = inputNumber.getText().toString().trim();
                String reason = inputReason.getText().toString().trim();

                layoutNumber.setError(null);
                layoutReason.setError(null);

                if (number.isEmpty()) {
                    layoutNumber.setError("Please confirm your student number");
                    return;
                }
                if (!number.equals(currentStudentProfile.getStudentNumber())) {
                    layoutNumber.setError("Student number does not match our records");
                    return;
                }
                if (reason.isEmpty()) {
                    layoutReason.setError("A reason is required");
                    return;
                }

                dialog.dismiss();
                showFinalDeletionWarning(reason); // Pass the reason to the next step
            });
        });

        dialog.show();
    }

    // --- Step 2 of Deletion (Updated Message) ---
    private void showFinalDeletionWarning(String reason) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("⚠️ Are you absolutely sure?")
                // --- THIS IS THE UPDATED MESSAGE ---
                .setMessage("Your request will be sent to an admin for permanent deletion. You will be logged out.\n\nTo appeal or cancel this request, please email alaokhemberly@gmail.com.\n\nThis action CANNOT BE REVERTED by you.")
                // --- END OF UPDATE ---
                .setPositiveButton("Yes, Request Deletion", (dialog, which) -> {
                    requestAccountDeletion(reason); // Pass the reason
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- Step 3 of Deletion (Updates Firestore with reason) ---
    private void requestAccountDeletion(String reason) {
        if (currentUser == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("isDeletionRequested", true);
        updates.put("deletionReason", reason); // <-- Save the reason

        db.collection("users").document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Deletion request sent. You will be logged out.", Toast.LENGTH_LONG).show();
                    logoutUser(); // Log user out
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to send request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void logoutUser() {
        if (getActivity() == null) return;
        mAuth.signOut();
        Intent intent = new Intent(getActivity(), LandingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }
}