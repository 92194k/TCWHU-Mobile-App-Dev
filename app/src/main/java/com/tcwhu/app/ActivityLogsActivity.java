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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ActivityLogsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ActivityLogsAdapter adapter;
    private List<ActivityLog> allLogList;
    private List<ActivityLog> filteredLogList;
    private FirebaseFirestore db;
    private TextView emptyView;
    private TextInputEditText inputSearch;
    private Spinner spinnerFilter;
    private String currentFilterType = "all";
    private String currentSearchQuery = "";

    // Temporary data model for the spinner filter options
    private static final String[] FILTER_OPTIONS = new String[]{"All Actions", "Approvals", "Bans & Suspensions", "Event Changes"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_logs);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.logsRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        inputSearch = findViewById(R.id.inputSearch);
        spinnerFilter = findViewById(R.id.spinnerFilter);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        allLogList = new ArrayList<>();
        filteredLogList = new ArrayList<>();

        setupRecyclerView();
        setupFilterSpinner();
        setupSearchListener();
        loadActivityLogs();
    }

    private void setupRecyclerView() {
        adapter = new ActivityLogsAdapter(filteredLogList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupFilterSpinner() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, FILTER_OPTIONS);
        spinnerFilter.setAdapter(spinnerAdapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFilterType = parent.getItemAtPosition(position).toString().toLowerCase().split(" ")[0]; // e.g., "all", "approvals"
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

    private void loadActivityLogs() {
        // NOTE: Logging is not automatically done by Firestore/Auth.
        // In a real app, admins would create a separate log entry for every action.
        // For now, we fetch from a fictional 'activity_logs' collection.

        db.collection("activity_logs").orderBy("timestamp", Query.Direction.DESCENDING).limit(100).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        allLogList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            ActivityLog log = document.toObject(ActivityLog.class);
                            log.setId(document.getId());
                            allLogList.add(log);
                        }
                        applyFilters();
                    } else {
                        Toast.makeText(this, "Error loading logs. Create 'activity_logs' collection in Firestore.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void applyFilters() {
        filteredLogList.clear();

        List<ActivityLog> searchResults = allLogList.stream()
                .filter(log -> log.getAction().toLowerCase().contains(currentSearchQuery.toLowerCase()))
                .filter(log -> {
                    String action = log.getAction().toLowerCase();
                    switch (currentFilterType) {
                        case "all":
                            return true;
                        case "approvals":
                            return action.contains("approv") || action.contains("verified") || action.contains("unban") || action.contains("unsuspend");
                        case "bans":
                            return action.contains("ban") || action.contains("suspend");
                        case "event":
                            return action.contains("event");
                        default:
                            return true;
                    }
                })
                .collect(Collectors.toList());

        filteredLogList.addAll(searchResults);
        adapter.notifyDataSetChanged();
        checkIfEmpty();
    }

    private void checkIfEmpty() {
        emptyView.setVisibility(filteredLogList.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(filteredLogList.isEmpty() ? View.GONE : View.VISIBLE);
    }
}