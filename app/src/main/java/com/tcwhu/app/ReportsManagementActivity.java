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
import java.util.List;

public class ReportsManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ReportsAdapter adapter;
    private List<Report> reportList;
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
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPendingReports();
    }

    private void setupRecyclerView() {
        reportList = new ArrayList<>();
        adapter = new ReportsAdapter(reportList, (report, action) -> {
            resolveReport(report, action);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
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
                        Toast.makeText(this, "Error loading reports.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resolveReport(Report report, String action) {
        // Update the report's status to "resolved" in Firestore
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