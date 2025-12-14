package com.tcwhu.app;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private String currentAdminNickname = "System Admin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_verification);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.verificationRecyclerView);
        emptyView = findViewById(R.id.emptyView);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setupRecyclerView();
        loadAdminNickname();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUnverifiedStudents();
    }

    private void loadAdminNickname() {
        FirebaseUser admin = FirebaseAuth.getInstance().getCurrentUser();
        if (admin != null) {
            db.collection("admins").document(admin.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("nickname");
                            if (name != null) currentAdminNickname = name;
                        }
                    });
        }
    }

    private void setupRecyclerView() {
        studentList = new ArrayList<>();
        adapter = new VerificationAdapter(studentList, new VerificationAdapter.OnActionListener() {
            @Override public void onApprove(Student student) { approveStudent(student); }
            @Override public void onReject(Student student) { rejectStudent(student); }
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
                    logAdminAction(currentAdminNickname, "Approved user: " + student.getNickname(), student.getUserId());
                    loadUnverifiedStudents();
                });
    }

    private void rejectStudent(Student student) {
        db.collection("users").document(student.getUserId()).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, student.getNickname() + " rejected.", Toast.LENGTH_SHORT).show();
                    logAdminAction(currentAdminNickname, "Rejected user: " + student.getNickname(), student.getUserId());
                    loadUnverifiedStudents();
                });
    }

    private void logAdminAction(String adminNickname, String action, String targetId) {
        Map<String, Object> log = new HashMap<>();
        log.put("adminId", adminNickname);
        log.put("action", action);
        log.put("targetId", targetId);
        log.put("timestamp", System.currentTimeMillis());
        db.collection("activity_logs").add(log);
    }

    private void checkIfEmpty() {
        emptyView.setVisibility(studentList.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(studentList.isEmpty() ? View.GONE : View.VISIBLE);
    }
}