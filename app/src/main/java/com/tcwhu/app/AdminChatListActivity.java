package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.util.Log; // FIX 1: ADD THIS IMPORT

public class AdminChatListActivity extends AppCompatActivity implements AdminChatListAdapter.OnChatClickListener {

    private RecyclerView recyclerView;
    private AdminChatListAdapter adapter;
    private List<Chat> chatList;
    private Map<String, Student> studentMap;
    private FirebaseFirestore db;
    private TextView emptyView;
    private ListenerRegistration chatListListener;

    // FIX 2: ADD THIS FIELD
    private static final String FALLBACK_ADMIN_ID = "admin_system_id_placeholder";

    // FIX 3: ADD THIS METHOD
    private String getAdminUserId() {
        try {
            // Attempt to access the constant dynamically
            return (String) ReportsManagementActivity.class.getField("ADMIN_USER_ID").get(null);
        } catch (Exception e) {
            // Fallback to the default placeholder ID if class or field is not found/public
            Log.e("AdminChatList", "Failed to resolve ADMIN_USER_ID from ReportsManagementActivity, using fallback.");
            return FALLBACK_ADMIN_ID;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_chat_list);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.usersRecyclerView);
        emptyView = findViewById(R.id.emptyView);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        chatList = new ArrayList<>();
        studentMap = new HashMap<>();
        adapter = new AdminChatListAdapter(chatList, studentMap, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllUsersAndThenChats();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (chatListListener != null) {
            chatListListener.remove();
        }
    }

    private void loadAllUsersAndThenChats() {
        db.collection("users").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    studentMap.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        // NOTE: Assuming 'Student' class exists and has setUserId
                        Student student = doc.toObject(Student.class);
                        student.setUserId(doc.getId());
                        studentMap.put(doc.getId(), student);
                    }
                    listenForAdminChats();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading user data.", Toast.LENGTH_SHORT).show());
    }

    private void listenForAdminChats() {
        String adminId = getAdminUserId(); // FIX: Now successfully calls the method

        if (chatListListener != null) chatListListener.remove();

        chatListListener = db.collection("chats")
                .whereArrayContains("users", adminId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) {
                        Log.e("AdminChatList", "Error loading chats: " + e.getMessage()); // FIX: Log is recognized
                        Toast.makeText(this, "Error loading chats.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    chatList.clear();

                    // Hardened logic to bypass corrupted documents
                    for (QueryDocumentSnapshot doc : snapshots) {
                        try {
                            Chat chat = doc.toObject(Chat.class);
                            chat.setChatId(doc.getId());
                            chatList.add(chat);
                        } catch (RuntimeException mapException) {
                            Log.e("AdminChatList", "Skipping corrupted chat document ID: " + doc.getId() + " due to deserialization error: " + mapException.getMessage()); // FIX: Log is recognized
                        }
                    }

                    adapter.notifyDataSetChanged();
                    checkIfEmpty();
                });
    }

    private void checkIfEmpty() {
        emptyView.setVisibility(chatList.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(chatList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onChatClick(Chat chat) {
        String adminId = getAdminUserId();

        // Find student ID
        String otherUserId = null;
        if (chat.getUsers() != null) {
            for (String id : chat.getUsers()) {
                if (!id.equals(adminId)) {
                    otherUserId = id;
                    break;
                }
            }
        }
        if (otherUserId == null) {
            Toast.makeText(this, "Error: Could not find student in chat.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mark as read
        if (chat.getLastSenderId() != null
                && !chat.getLastSenderId().equals(adminId)
                && !chat.isRead()) {

            String actualChatId = chat.getChatId();

            if (actualChatId != null) {
                db.collection("chats").document(actualChatId).update("read", true)
                        .addOnFailureListener(e -> Log.e("AdminChatList", "Failed to mark chat as read.", e));
            }
        }

        // Navigate to ChatWindowActivity
        Intent intent = new Intent(this, ChatWindowActivity.class);
        intent.putExtra("ADMIN_USER_ID", adminId);
        intent.putExtra(ChatWindowActivity.EXTRA_OTHER_USER_ID, otherUserId);
        startActivity(intent);
    }
}