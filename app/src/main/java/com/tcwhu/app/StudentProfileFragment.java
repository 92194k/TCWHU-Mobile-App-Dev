package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
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

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private Student currentStudentProfile;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_profile, container, false);

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

        loadUserProfile();

        buttonLogout.setOnClickListener(v -> logoutUser());
        buttonDeleteAccount.setOnClickListener(v -> showDeleteConfirmationDialog());
        buttonChangeAvatar.setOnClickListener(v -> Toast.makeText(getContext(), "Change Avatar clicked (WIP)", Toast.LENGTH_SHORT).show());

        buttonEditNickname.setOnClickListener(v -> showEditFieldDialog("nickname", currentStudentProfile.getNickname()));
        buttonEditInterests.setOnClickListener(v -> showEditFieldDialog("interests", currentStudentProfile.getInterests()));
        buttonChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        return view;
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
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load profile.", Toast.LENGTH_SHORT).show();
                });
    }

    // --- Dialogs and Update Logic ---

    private void showEditFieldDialog(String fieldName, String currentValue) {
        if (getContext() == null) return;

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
                    loadUserProfile(); // Reload data to update UI
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showChangePasswordDialog() {
        if (getContext() == null || currentUser == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, null);
        EditText inputCurrentPassword = dialogView.findViewById(R.id.inputCurrentPassword);
        EditText inputNewPassword = dialogView.findViewById(R.id.inputNewPassword);
        EditText inputConfirmPassword = dialogView.findViewById(R.id.inputConfirmPassword);

        new AlertDialog.Builder(getContext())
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String current = inputCurrentPassword.getText().toString();
                    String newPass = inputNewPassword.getText().toString();
                    String confirmPass = inputConfirmPassword.getText().toString();

                    if (!newPass.equals(confirmPass)) {
                        Toast.makeText(getContext(), "New passwords do not match.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPass.length() < 6) {
                        Toast.makeText(getContext(), "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    reauthenticateAndChangePassword(current, newPass);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void reauthenticateAndChangePassword(String currentPassword, String newPassword) {
        String email = currentUser.getEmail(); // The dummy email (e.g., studentnumber@tcwhu.app)
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

    private void showDeleteConfirmationDialog() {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Request Account Deletion")
                .setMessage("Are you sure you want to request account deletion? This action is permanent and will be reviewed by an admin.")
                .setPositiveButton("Request Deletion", (dialog, which) -> {
                    // In a real app, this would update a 'deletionRequested' field in Firestore
                    Toast.makeText(getContext(), "Deletion request sent to admin.", Toast.LENGTH_LONG).show();
                    logoutUser();
                })
                .setNegativeButton("Cancel", null)
                .show();
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