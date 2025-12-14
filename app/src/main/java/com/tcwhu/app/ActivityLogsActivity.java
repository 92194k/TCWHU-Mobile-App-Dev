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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ActivityLogsActivity extends AppCompatActivity implements ActivityLogsAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private ActivityLogsAdapter adapter;
    private List<ActivityLog> allLogList;
    private List<ActivityLog> filteredLogList;
    private FirebaseFirestore db;
    private TextView emptyView;

    private TextInputEditText inputSearch;
    private Spinner spinnerFilter;
    private String currentFilter = "all";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_logs);

        db = FirebaseFirestore.getInstance();
        initViews();

        allLogList = new ArrayList<>();
        filteredLogList = new ArrayList<>();

        setupRecyclerView();
        setupFilterSpinner();
        setupSearchListener();
        loadActivityLogs();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.logsRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        inputSearch = findViewById(R.id.inputSearch);
        spinnerFilter = findViewById(R.id.spinnerFilter);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new ActivityLogsAdapter(filteredLogList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupFilterSpinner() {
        String[] filters = new String[]{"All Logs", "Approvals", "Rejections", "Bans", "Updates"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filters);
        spinnerFilter.setAdapter(spinnerAdapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFilter = parent.getItemAtPosition(position).toString().toLowerCase();
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
                currentSearchQuery = s.toString().toLowerCase();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadActivityLogs() {
        db.collection("activity_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
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
                        Toast.makeText(this, "Error loading logs.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void applyFilters() {
        filteredLogList.clear();

        List<ActivityLog> searchResults = allLogList.stream()
                .filter(log -> {
                    boolean matchesSearch = (log.getAction() != null && log.getAction().toLowerCase().contains(currentSearchQuery)) ||
                            (log.getAdminId() != null && log.getAdminId().toLowerCase().contains(currentSearchQuery));

                    if (!matchesSearch) return false;

                    switch (currentFilter) {
                        case "approvals": return log.getAction().toLowerCase().contains("approve");
                        case "rejections": return log.getAction().toLowerCase().contains("reject");
                        case "bans": return log.getAction().toLowerCase().contains("ban");
                        case "updates": return log.getAction().toLowerCase().contains("update");
                        case "all logs": default: return true;
                    }
                })
                .collect(Collectors.toList());

        filteredLogList.addAll(searchResults);
        adapter.notifyDataSetChanged();

        emptyView.setVisibility(filteredLogList.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(filteredLogList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onItemClick(ActivityLog log) {
        Intent intent = new Intent(this, ActivityLogDetailActivity.class);
        intent.putExtra(ActivityLogDetailActivity.EXTRA_LOG, log);
        startActivity(intent);
    }
}