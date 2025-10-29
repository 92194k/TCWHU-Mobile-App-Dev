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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentVerificationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private VerificationAdapter adapter;
    private List<Student> studentList;
    private FirebaseFirestore db;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_verification);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.verificationRecyclerView);
        emptyView = findViewById(R.id.emptyView);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUnverifiedStudents();
    }

    private void setupRecyclerView() {
        studentList = new ArrayList<>();
        adapter = new VerificationAdapter(studentList, new VerificationAdapter.OnActionListener() {
            @Override
            public void onApprove(Student student) {
                approveStudent(student);
            }
            @Override
            public void onReject(Student student) {
                rejectStudent(student);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadUnverifiedStudents() {
        db.collection("users").whereEqualTo("isVerified", false).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        studentList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Student student = document.toObject(Student.class);
                            student.setUserId(document.getId());
                            studentList.add(student);
                        }
                        adapter.notifyDataSetChanged();
                        checkIfEmpty();
                    } else {
                        Toast.makeText(this, "Error loading students.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void approveStudent(Student student) {
        db.collection("users").document(student.getUserId()).update("isVerified", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, student.getNickname() + " approved.", Toast.LENGTH_SHORT).show();
                    // --- ACTIVITY LOGGING ---
                    logAdminAction("Approved user: " + student.getNickname(), student.getUserId());
                    loadUnverifiedStudents(); // Refresh the list
                });
    }

    private void rejectStudent(Student student) {
        db.collection("users").document(student.getUserId()).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, student.getNickname() + " rejected and removed.", Toast.LENGTH_SHORT).show();
                    // --- ACTIVITY LOGGING ---
                    logAdminAction("Rejected and removed user: " + student.getNickname(), student.getUserId());
                    loadUnverifiedStudents(); // Refresh the list
                });
    }

    // --- NEW METHOD TO SAVE LOGS TO FIRESTORE ---
    private void logAdminAction(String action, String targetId) {
        // In a real app, you would get the admin's actual ID/name
        String adminId = "admin_user";

        Map<String, Object> log = new HashMap<>();
        log.put("adminId", adminId);
        log.put("action", action);
        log.put("targetId", targetId);
        log.put("timestamp", System.currentTimeMillis());

        // Save to the 'activity_logs' collection in Firestore
        db.collection("activity_logs").add(log);
    }

    private void checkIfEmpty() {
        if (emptyView != null) {
            emptyView.setVisibility(studentList.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(studentList.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }
}