package com.tcwhu.app;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportsManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ReportsAdapter adapter;
    private List<Report> reportList;
    private Map<String, String> userNicknameMap;
    private FirebaseFirestore db;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports_management);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.reportsRecyclerView);
        emptyView = findViewById(R.id.emptyView);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllUsersAndThenReports(); // Start the data loading chain
    }

    private void setupRecyclerView() {
        reportList = new ArrayList<>();
        userNicknameMap = new HashMap<>();
        adapter = new ReportsAdapter(reportList, userNicknameMap, (report, action) -> {
            resolveReport(report, action);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadAllUsersAndThenReports() {
        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                userNicknameMap.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    userNicknameMap.put(document.getId(), document.getString("nickname"));
                }
                // After we have all user nicknames, load the reports
                loadPendingReports();
            } else {
                Toast.makeText(this, "Error loading user data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPendingReports() {
        db.collection("reports").whereEqualTo("status", "pending").orderBy("timestamp", Query.Direction.DESCENDING).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        reportList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Report report = document.toObject(Report.class);
                            report.setId(document.getId());
                            reportList.add(report);
                        }
                        adapter.notifyDataSetChanged();
                        checkIfEmpty();
                    } else {
                        Toast.makeText(this, "Error loading reports. Check Firestore Rules and Index.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void resolveReport(Report report, String action) {
        db.collection("reports").document(report.getId()).update("status", "resolved_action_" + action)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Action '" + action + "' taken. Report resolved.", Toast.LENGTH_SHORT).show();
                    loadPendingReports(); // Refresh the list
                });
    }

    private void checkIfEmpty() {
        emptyView.setVisibility(reportList.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(reportList.isEmpty() ? View.GONE : View.VISIBLE);
    }
}