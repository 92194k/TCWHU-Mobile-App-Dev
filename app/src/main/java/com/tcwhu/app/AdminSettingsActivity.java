package com.tcwhu.app;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

public class AdminSettingsActivity extends AppCompatActivity implements AdminAccountsAdapter.OnRemoveListener {

    private RecyclerView recyclerView;
    private AdminAccountsAdapter adapter;
    private List<AdminAccount> adminList;
    private FirebaseFirestore db;
    private TextView textCurrentAccess;

    private static final String ACCESS_CODE_DOC_ID = "GLOBAL_ACCESS_CODE";
    private String currentAccessCode = "123456";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_settings);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.adminsRecyclerView);
        textCurrentAccess = findViewById(R.id.textCurrentAccess);
        Button buttonChangeAccess = findViewById(R.id.buttonChangeAccess);
        Button buttonAddAdmin = findViewById(R.id.buttonAddAdmin);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setupRecyclerView();
        loadAccessCode();
        loadAdminAccounts();

        buttonChangeAccess.setOnClickListener(v -> showChangeAccessCodeDialog());
        buttonAddAdmin.setOnClickListener(v -> showAddAdminDialog());
    }

    // Access Code Logic
    private void loadAccessCode() {
        db.collection("settings").document(ACCESS_CODE_DOC_ID).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.getString("code") != null) {
                        currentAccessCode = documentSnapshot.getString("code");
                    }
                    updateAccessCodeDisplay();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load access code.", Toast.LENGTH_SHORT).show();
                    updateAccessCodeDisplay();
                });
    }

    private void updateAccessCodeDisplay() {
        // Masked access code
        String maskedCode = new String(new char[currentAccessCode.length()]).replace('\0', '•');
        textCurrentAccess.setText(maskedCode);
    }

    private void saveAccessCode(String newCode) {
        Map<String, Object> data = new HashMap<>();
        data.put("code", newCode);
        db.collection("settings").document(ACCESS_CODE_DOC_ID).set(data)
                .addOnSuccessListener(aVoid -> {
                    currentAccessCode = newCode;
                    updateAccessCodeDisplay();
                    Toast.makeText(this, "Access code has been updated successfully.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Unable to update the access code. Please try again", Toast.LENGTH_SHORT).show());
    }

    private void showChangeAccessCodeDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_access_code, null);
        TextInputEditText input = dialogView.findViewById(R.id.inputNewAccessCode);
        Button buttonUpdate = dialogView.findViewById(R.id.buttonUpdateAccess);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        buttonUpdate.setOnClickListener(v -> {
            String newCode = input.getText().toString().trim();
            if (newCode.length() >= 4) {
                saveAccessCode(newCode);
                dialog.dismiss();
            } else {
                input.setError("Access code must be at least 4 digits.");
            }
        });
        dialog.show();
    }

    // Admin Account Logic
    private void setupRecyclerView() {
        adminList = new ArrayList<>();
        adapter = new AdminAccountsAdapter(adminList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadAdminAccounts() {
        db.collection("admins").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        adminList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            AdminAccount admin = document.toObject(AdminAccount.class);
                            admin.setId(document.getId());
                            adminList.add(admin);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Unable to load admin accounts.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAddAdminDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_admin, null);
        TextInputEditText inputEmail = dialogView.findViewById(R.id.inputAdminEmail);
        TextInputEditText inputRole = dialogView.findViewById(R.id.inputAdminRole);
        Button buttonAdd = dialogView.findViewById(R.id.buttonAddAdmin);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        buttonAdd.setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();
            String role = inputRole.getText().toString().trim();
            if (!email.isEmpty() && !role.isEmpty()) {
                addAdminAccount(email, role);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please complete all required fields.", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    private void addAdminAccount(String email, String role) {
        AdminAccount newAdmin = new AdminAccount();
        newAdmin.setEmail(email);
        newAdmin.setRole(role);
        newAdmin.setAddedDate(System.currentTimeMillis());

        // Adding Admin
        db.collection("admins").add(newAdmin)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Admin account has been added successfully.", Toast.LENGTH_SHORT).show();
                    loadAdminAccounts();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Unable to add admin account. Please try again", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onRemoveClick(AdminAccount admin) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Admin?")
                .setMessage("This will permanently revoke admin access for " + admin.getEmail() + ".")
                .setPositiveButton("Remove", (dialog, which) -> deleteAdminAccount(admin))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAdminAccount(AdminAccount admin) {
        db.collection("admins").document(admin.getId()).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, admin.getEmail() + " access revoked.", Toast.LENGTH_SHORT).show();
                    loadAdminAccounts();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to remove admin access.", Toast.LENGTH_SHORT).show());
    }
}