package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ReportsManagementActivity extends AppCompatActivity implements ReportsAdapter.OnActionListener {

    private RecyclerView recyclerView;
    private ReportsAdapter adapter;
    private List<Report> reportList;
    private Map<String, String> userNicknameMap;
    private FirebaseFirestore db;
    private TextView emptyView;

    public static final String ADMIN_USER_ID = "system_admin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports_management);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.reportsRecyclerView);
        emptyView = findViewById(R.id.emptyView);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllUsersAndThenReports();
    }

    private void setupRecyclerView() {
        reportList = new ArrayList<>();
        userNicknameMap = new HashMap<>();
        adapter = new ReportsAdapter(reportList, userNicknameMap, this);
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
                loadPendingReports();
            } else {
                Toast.makeText(this, "Error loading user data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPendingReports() {
        db.collection("reports")
                .whereEqualTo("status", "pending")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
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
                        Toast.makeText(this, "Error loading reports.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onActionClick(Report report, String action) {
        String reportedUserId = report.getReportedUserId();
        if (reportedUserId == null || reportedUserId.isEmpty()) {
            Toast.makeText(this, "Invalid user ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        switch (action) {
            case "warning":
                showWarningDialog(report);
                break;
            case "suspend":
                showConfirmationDialog("Suspend User", "Suspend this user for 30 days?", report, action);
                break;
            case "ban":
                showConfirmationDialog("Permanent Ban", "Permanently ban this user?", report, action);
                break;
        }
    }

    private void showWarningDialog(Report report) {
        final EditText inputReason = new EditText(this);
        inputReason.setHint("Enter warning (e.g., 'Inappropriate language')");
        inputReason.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(this)
                .setTitle("Issue Warning")
                .setMessage("This will send a warning to the user via chat.")
                .setView(inputReason)
                .setPositiveButton("Send Warning", (dialog, which) -> {
                    String reason = inputReason.getText().toString().trim();
                    if (!reason.isEmpty()) executeAction(report, "warning", reason);
                    else Toast.makeText(this, "A warning message is required.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showConfirmationDialog(String title, String message, Report report, String action) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Confirm", (dialog, which) -> executeAction(report, action, null))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeAction(Report report, String action, String warningReason) {
        String reportedUserId = report.getReportedUserId();
        WriteBatch batch = db.batch();
        DocumentReference userRef = db.collection("users").document(reportedUserId);
        Map<String, Object> userUpdates = new HashMap<>();

        if ("warning".equals(action)) {
            userUpdates.put("warningCount", FieldValue.increment(1));
            batch.update(userRef, userUpdates);

            Intent intent = new Intent(this, ChatWindowActivity.class);
            intent.putExtra("ADMIN_USER_ID", ADMIN_USER_ID);
            intent.putExtra(ChatWindowActivity.EXTRA_OTHER_USER_ID, reportedUserId);
            String warningTemplate = "Official Warning: " + warningReason + "\n\nPlease review our community guidelines.";
            intent.putExtra("WARNING_TEMPLATE", warningTemplate);
            startActivity(intent);

        } else if ("suspend".equals(action)) {
            long suspendMillis = TimeUnit.DAYS.toMillis(30);
            userUpdates.put("isSuspended", true);
            userUpdates.put("suspendEndDate", System.currentTimeMillis() + suspendMillis);
            userUpdates.put("isBanned", false);
            batch.update(userRef, userUpdates);

        } else if ("ban".equals(action)) {
            userUpdates.put("isBanned", true);
            userUpdates.put("isSuspended", false);
            batch.update(userRef, userUpdates);
        }

        DocumentReference reportRef = db.collection("reports").document(report.getId());
        batch.update(reportRef, "status", "resolved_action_" + action);
        batch.update(reportRef, "resolved", true);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Action taken. Report resolved.", Toast.LENGTH_SHORT).show();
                    loadPendingReports();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Action failed.", Toast.LENGTH_SHORT).show());
    }

    private void checkIfEmpty() {
        emptyView.setVisibility(reportList.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(reportList.isEmpty() ? View.GONE : View.VISIBLE);
    }
}