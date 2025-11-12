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
        loadAllUsersAndThenReports();
    }

    private void setupRecyclerView() {
        reportList = new ArrayList<>();
        userNicknameMap = new HashMap<>();
        adapter = new ReportsAdapter(reportList, userNicknameMap, (report, action) -> {
            handleAdminAction(report, action);
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
                        Toast.makeText(this, "Error loading reports. Check Firestore Rules and Index.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkIfEmpty() {
        emptyView.setVisibility(reportList.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(reportList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /**
     * Handles admin action: warning, suspend, or ban.
     */
    private void handleAdminAction(Report report, String action) {
        String reportedUserId = report.getReportedUserId();
        if (reportedUserId == null || reportedUserId.isEmpty()) {
            Toast.makeText(this, "Invalid user ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        switch (action) {
            case "warning":
                issueWarning(reportedUserId, report);
                break;

            case "suspend":
                suspendUser(reportedUserId, report);
                break;

            case "ban":
                banUser(reportedUserId, report);
                break;
        }
    }

    /**
     * Issue a warning to the user.
     * Increments warningCount and notifies the user.
     */
    private void issueWarning(String userId, Report report) {
        db.collection("users").document(userId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Integer currentCount = snapshot.getLong("warningCount") != null
                        ? snapshot.getLong("warningCount").intValue() : 0;

                int newCount = currentCount + 1;

                db.collection("users").document(userId)
                        .update("warningCount", newCount)
                        .addOnSuccessListener(aVoid -> {
                            resolveReport(report, "warning");
                            // Send a notification entry
                            Map<String, Object> notif = new HashMap<>();
                            notif.put("title", "Account Warning Issued");
                            notif.put("message", "You have received a warning from the admin. Total warnings: " + newCount);
                            notif.put("timestamp", System.currentTimeMillis());
                            notif.put("userId", userId);
                            db.collection("notifications").add(notif);

                            Toast.makeText(this, "Warning issued and user notified.", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to issue warning.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * Suspend user for 30 days.
     */
    private void suspendUser(String userId, Report report) {
        long suspendEndDate = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000); // 30 days in ms
        Map<String, Object> update = new HashMap<>();
        update.put("isSuspended", true);
        update.put("suspendEndDate", suspendEndDate);

        db.collection("users").document(userId)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    resolveReport(report, "suspend");

                    // Log a notification
                    Map<String, Object> notif = new HashMap<>();
                    notif.put("title", "Account Suspended");
                    notif.put("message", "Your account has been suspended for 30 days. Access will be restored on " +
                            new java.text.SimpleDateFormat("MMM d, yyyy").format(new java.util.Date(suspendEndDate)) + ".");
                    notif.put("timestamp", System.currentTimeMillis());
                    notif.put("userId", userId);
                    db.collection("notifications").add(notif);

                    Toast.makeText(this, "User suspended for 30 days.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to suspend user.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Permanently bans the user.
     */
    private void banUser(String userId, Report report) {
        Map<String, Object> update = new HashMap<>();
        update.put("isBanned", true);
        update.put("deletionDate", System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000)); // optional auto-delete in 90 days
        update.put("deletionReason", "Permanent ban for violation of rules");

        db.collection("users").document(userId)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    resolveReport(report, "ban");

                    // Add a notification
                    Map<String, Object> notif = new HashMap<>();
                    notif.put("title", "Account Permanently Banned");
                    notif.put("message", "Your account has been permanently banned due to policy violations.");
                    notif.put("timestamp", System.currentTimeMillis());
                    notif.put("userId", userId);
                    db.collection("notifications").add(notif);

                    Toast.makeText(this, "User permanently banned.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to ban user.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Marks the report as resolved.
     */
    private void resolveReport(Report report, String action) {
        db.collection("reports").document(report.getId())
                .update("status", "resolved_action_" + action)
                .addOnSuccessListener(aVoid -> loadPendingReports())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to resolve report.", Toast.LENGTH_SHORT).show());
    }
}
