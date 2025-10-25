package com.tcwhu.app;

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
import java.util.List;
import java.util.stream.Collectors;

public class UserOverviewActivity extends AppCompatActivity implements UserOverviewAdapter.OnActionListener {

    private RecyclerView recyclerView;
    private UserOverviewAdapter adapter;
    private List<Student> allStudentList; // Full list from Firestore
    private List<Student> filteredStudentList; // List currently shown
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
        setupFilterSpinner();
        setupSearchListener();
        loadAllStudents();
    }

    private void setupRecyclerView() {
        adapter = new UserOverviewAdapter(filteredStudentList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupFilterSpinner() {
        String[] filters = new String[]{"All Users", "Verified Only", "Pending Only", "Suspended Only", "Banned Only"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filters);
        spinnerFilter.setAdapter(spinnerAdapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFilter = parent.getItemAtPosition(position).toString().toLowerCase().split(" ")[0]; // Extracts "all", "verified", etc.
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
        // Apply filtering and searching on the client side (since we fetched all users)
        filteredStudentList.clear();

        List<Student> searchResults = allStudentList.stream()
                .filter(student ->
                        student.getNickname().toLowerCase().contains(currentSearchQuery.toLowerCase()) ||
                                student.getStudentNumber().toLowerCase().contains(currentSearchQuery.toLowerCase()))
                .filter(student -> {
                    switch (currentFilter) {
                        case "verified": return student.isVerified() && !student.isBanned() && !student.isSuspended();
                        case "pending": return !student.isVerified() && !student.isBanned() && !student.isSuspended();
                        case "suspended": return student.isSuspended();
                        case "banned": return student.isBanned();
                        case "all":
                        default: return true;
                    }
                })
                .collect(Collectors.toList());

        filteredStudentList.addAll(searchResults);
        adapter.notifyDataSetChanged();
        checkIfEmpty();
    }

    // --- Action Listeners (Implement UserOverviewAdapter.OnActionListener) ---

    @Override
    public void onBan(String userId) {
        showConfirmationDialog("Ban User", "Are you sure you want to permanently ban this user?", userId, "ban", "#B71C1C");
    }

    @Override
    public void onUnsuspend(String userId) {
        showConfirmationDialog("Unsuspend User", "Are you sure you want to lift the suspension for this user?", userId, "unsuspend", "#388E3C");
    }

    @Override
    public void onUnban(String userId) {
        showConfirmationDialog("Unban User", "Are you sure you want to unban this user and restore their access?", userId, "unban", "#388E3C");
    }

    @Override
    public void onDelete(String userId) {
        showConfirmationDialog("Delete Account", "WARNING: This action is permanent and removes the user from the database and authentication system.", userId, "delete", "#D32F2F");
    }

    // --- Database Action Execution ---

    private void showConfirmationDialog(String title, String message, String userId, String action, String colorHex) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(action.toUpperCase(), (dialog, which) -> executeUserAction(userId, action))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeUserAction(String userId, String action) {
        if ("delete".equals(action)) {
            // Deleting from Firestore is simple, but deleting from Auth requires a backend function (beyond mobile scope)
            db.collection("users").document(userId).delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "User deleted from Firestore.", Toast.LENGTH_LONG).show();
                        loadAllStudents(); // Refresh the list
                    });
            return;
        }

        // Map actions to Firestore updates
        String field = "";
        Boolean value = null;

        switch (action) {
            case "ban":
                field = "isBanned";
                value = true;
                break;
            case "unsuspend":
                field = "isSuspended";
                value = false;
                break;
            case "unban":
                field = "isBanned";
                value = false;
                break;
            default:
                return;
        }

        db.collection("users").document(userId).update(field, value)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "User status updated to " + action.toUpperCase() + ".", Toast.LENGTH_SHORT).show();
                    loadAllStudents(); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update user status.", Toast.LENGTH_SHORT).show();
                });
    }


    private void checkIfEmpty() {
        emptyView.setVisibility(filteredStudentList.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(filteredStudentList.isEmpty() ? View.GONE : View.VISIBLE);
    }
}