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

public class AdminChatListActivity extends AppCompatActivity implements AdminChatListAdapter.OnChatClickListener {

    private RecyclerView recyclerView;
    private AdminChatListAdapter adapter;
    private List<Chat> chatList;
    private Map<String, Student> studentMap;
    private FirebaseFirestore db;
    private TextView emptyView;
    private ListenerRegistration chatListListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_chat_list);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.usersRecyclerView);
        emptyView = findViewById(R.id.emptyView);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
                        Student student = doc.toObject(Student.class);
                        student.setUserId(doc.getId());
                        studentMap.put(doc.getId(), student);
                    }
                    listenForAdminChats();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading user data.", Toast.LENGTH_SHORT).show());
    }

    private void listenForAdminChats() {
        if (chatListListener != null) chatListListener.remove();

        chatListListener = db.collection("chats")
                .whereArrayContains("users", ReportsManagementActivity.ADMIN_USER_ID)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) {
                        Toast.makeText(this, "Error loading chats.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    chatList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Chat chat = doc.toObject(Chat.class);
                        // ✅ FIX 1: Store the Firestore document ID in the Chat object
                        chat.setChatId(doc.getId());
                        chatList.add(chat);
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
        // Find student ID
        String otherUserId = null;
        if (chat.getUsers() != null) {
            for (String id : chat.getUsers()) {
                if (!id.equals(ReportsManagementActivity.ADMIN_USER_ID)) {
                    otherUserId = id;
                    break;
                }
            }
        }
        if (otherUserId == null) {
            Toast.makeText(this, "Error: Could not find student in chat.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ FIX 2: Mark messages as read using the actual Firestore document ID
        // Only mark as read if the last message was NOT sent by the admin AND it's currently unread
        if (chat.getLastSenderId() != null
                && !chat.getLastSenderId().equals(ReportsManagementActivity.ADMIN_USER_ID)
                && !chat.isRead()) {

            String actualChatId = chat.getChatId();

            if (actualChatId != null) {
                db.collection("chats").document(actualChatId).update("read", true)
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to mark chat as read.", Toast.LENGTH_SHORT).show());
            }
        }

        Intent intent = new Intent(this, ChatWindowActivity.class);
        intent.putExtra("ADMIN_USER_ID", ReportsManagementActivity.ADMIN_USER_ID);
        intent.putExtra(ChatWindowActivity.EXTRA_OTHER_USER_ID, otherUserId);
        startActivity(intent);
    }
}