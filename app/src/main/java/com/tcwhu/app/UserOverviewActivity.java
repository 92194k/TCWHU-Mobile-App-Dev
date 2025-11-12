package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserOverviewActivity extends AppCompatActivity implements UserOverviewAdapter.OnActionListener {

    private RecyclerView recyclerView;
    private UserOverviewAdapter adapter;
    private List<Student> allStudentList;
    private List<Student> filteredStudentList;
    private FirebaseFirestore db;
    private TextView emptyView;
    private TextInputEditText inputSearch;
    private Spinner spinnerFilter;
    private String currentFilter = "all";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_overview);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.usersRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        inputSearch = findViewById(R.id.inputSearch);
        spinnerFilter = findViewById(R.id.spinnerFilter);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        allStudentList = new ArrayList<>();
        filteredStudentList = new ArrayList<>();

        setupRecyclerView();
        setupFilterSpinner(); // <-- UPDATED
        setupSearchListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllStudents();
    }

    private void setupRecyclerView() {
        adapter = new UserOverviewAdapter(filteredStudentList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupFilterSpinner() {
        // --- ADDED NEW FILTER ---
        String[] filters = new String[]{"All Users", "Verified Only", "Pending Only", "Suspended Only", "Banned Only", "Deletion Requests"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filters);
        spinnerFilter.setAdapter(spinnerAdapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // "Deletion Requests" becomes "deletion"
                currentFilter = parent.getItemAtPosition(position).toString().toLowerCase().split(" ")[0];
                applyFilters();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSearchListener() {
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadAllStudents() {
        db.collection("users").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        allStudentList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // This uses the new fixed Student.java
                            Student student = document.toObject(Student.class);
                            student.setUserId(document.getId());
                            allStudentList.add(student);
                        }
                        applyFilters();
                    } else {
                        Toast.makeText(this, "Error loading users.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void applyFilters() {
        filteredStudentList.clear();

        List<Student> searchResults = allStudentList.stream()
                .filter(student ->
                        (student.getNickname() != null && student.getNickname().toLowerCase().contains(currentSearchQuery.toLowerCase())) ||
                                (student.getStudentNumber() != null && student.getStudentNumber().toLowerCase().contains(currentSearchQuery.toLowerCase())))
                .filter(student -> {
                    boolean isVerified = student.isVerified();
                    boolean isBanned = student.isBanned();
                    boolean isSuspended = student.isSuspended();
                    boolean isDeletionRequested = student.isDeletionRequested(); // <-- ADDED

                    switch (currentFilter) {
                        case "verified": return isVerified && !isBanned && !isSuspended;
                        case "pending": return !isVerified && !isBanned && !isSuspended;
                        case "suspended": return isSuspended;
                        case "banned": return isBanned;
                        case "deletion": return isDeletionRequested; // <-- ADDED
                        case "all":
                        default: return true;
                    }
                })
                .collect(Collectors.toList());

        filteredStudentList.addAll(searchResults);
        adapter.notifyDataSetChanged();
        checkIfEmpty();
    }

    private void checkIfEmpty() {
        emptyView.setVisibility(filteredStudentList.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(filteredStudentList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // --- ACTION HANDLERS ---

    @Override
    public void onSuspend(String userId) {
        showConfirmationDialog("Suspend User", "Suspend user for 7 days?", userId, "suspend");
    }

    @Override
    public void onBan(String userId) {
        showConfirmationDialog("Ban User", "Ban user? Account will be deleted in 30 days.", userId, "ban");
    }

    @Override
    public void onUnsuspend(String userId) {
        showConfirmationDialog("Unsuspend User", "Are you sure you want to lift this suspension?", userId, "unsuspend");
    }

    @Override
    public void onUnban(String userId) {
        showConfirmationDialog("Unban User", "Are you sure you want to unban this user?", userId, "unban");
    }

    @Override
    public void onDelete(String userId) {
        // This is now "Approve Deletion" or "Force Delete"
        showConfirmationDialog("Approve Deletion", "This will permanently remove this user's data and account immediately. Continue?", userId, "delete");
    }

    @Override
    public void onReview(Student student) {
        Intent intent = new Intent(this, StudentVerificationActivity.class);
        intent.putExtra("studentId", student.getUserId());
        startActivity(intent);
    }

    // --- ADDED: Handle the "Deny" button click ---
    @Override
    public void onDenyDeletion(String userId) {
        showConfirmationDialog("Deny Request", "Deny this user's account deletion request?", userId, "deny_deletion");
    }


    private void showConfirmationDialog(String title, String message, String userId, String action) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("YES", (dialog, which) -> executeUserAction(userId, action))
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void executeUserAction(String userId, String action) {
        if ("delete".equals(action)) {
            // This triggers your "deleteUserAccount" Cloud Function
            db.collection("users").document(userId).delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "User deleted.", Toast.LENGTH_SHORT).show();
                        loadAllStudents();
                    });
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        long now = System.currentTimeMillis();

        switch (action) {
            case "suspend":
                long suspendTime = 7 * 24 * 60 * 60 * 1000L;
                updates.put("isSuspended", true);
                updates.put("suspendEndDate", now + suspendTime);
                break;
            case "ban":
                long banTime = 30 * 24 * 60 * 60 * 1000L;
                updates.put("isBanned", true);
                updates.put("isSuspended", false);
                updates.put("deletionDate", now + banTime);
                break;
            case "unsuspend":
                updates.put("isSuspended", false);
                updates.put("suspendEndDate", 0L);
                break;
            case "unban":
                updates.put("isBanned", false);
                updates.put("deletionDate", 0L);
                break;
            // --- ADDED: The "Deny" action ---
            case "deny_deletion":
                updates.put("isDeletionRequested", false);
                updates.put("deletionReason", null); // Optional: clear the reason
                break;
        }

        db.collection("users").document(userId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "User " + action + " successfully.", Toast.LENGTH_SHORT).show();
                    loadAllStudents();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update user.", Toast.LENGTH_SHORT).show());
    }
}