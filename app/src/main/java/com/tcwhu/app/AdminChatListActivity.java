package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import java.util.Arrays;
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

    private static final String FALLBACK_ADMIN_ID = "admin_system_id_placeholder";
    private static final String ADMIN_ROLE = "Super Admin";

    private String getAdminUserId() {
        try {
            return (String) ReportsManagementActivity.class.getField("ADMIN_USER_ID").get(null);
        } catch (Exception e) {
            Log.e("AdminChatList", "Failed to resolve ADMIN_USER_ID, using fallback.");
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
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
        if (chatListListener != null) chatListListener.remove();
    }

    private void loadAllUsersAndThenChats() {
        db.collection("users").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    studentMap.clear();
                    List<String> allActiveStudentIds = new ArrayList<>();
                    String currentAdminId = getAdminUserId();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Student student = doc.toObject(Student.class);
                        String userId = doc.getId();
                        student.setUserId(userId);

                        studentMap.put(userId, student);

                        if (!userId.equals(currentAdminId) &&
                                (student.getRole() == null || !student.getRole().equals(ADMIN_ROLE))) {
                            allActiveStudentIds.add(userId);
                        }
                    }
                    listenForAdminChats(allActiveStudentIds);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading user data.", Toast.LENGTH_SHORT).show());
    }

    private void listenForAdminChats(List<String> allStudentIds) {
        String adminId = getAdminUserId();
        if (chatListListener != null) chatListListener.remove();

        chatListListener = db.collection("chats")
                .whereArrayContains("users", adminId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    chatList.clear();
                    List<String> threadsFound = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        try {
                            Chat chat = doc.toObject(Chat.class);
                            String otherUserId = findOtherUserId(chat, adminId);

                            if (studentMap.containsKey(otherUserId)) {
                                chat.setChatId(doc.getId());
                                chatList.add(chat);
                                threadsFound.add(otherUserId);
                            }
                        } catch (RuntimeException ignored) {}
                    }

                    // Add dummy chats for users without history
                    for (String studentId : allStudentIds) {
                        if (!threadsFound.contains(studentId)) {
                            Student student = studentMap.get(studentId);
                            if (student != null) {
                                Chat dummyChat = new Chat();
                                dummyChat.setUsers(Arrays.asList(adminId, studentId));
                                dummyChat.setLastMessage("Start Chat");
                                dummyChat.setTimestamp(0);
                                dummyChat.setRead(true);
                                dummyChat.setChatId(null);
                                chatList.add(dummyChat);
                            }
                        }
                    }

                    chatList.sort((c1, c2) -> Long.compare(c2.getTimestamp(), c1.getTimestamp()));
                    adapter.notifyDataSetChanged();
                    checkIfEmpty();
                });
    }

    private String findOtherUserId(Chat chat, String currentId) {
        if (chat.getUsers() == null) return null;
        for (String id : chat.getUsers()) {
            if (!id.equals(currentId)) return id;
        }
        return null;
    }

    private void checkIfEmpty() {
        emptyView.setVisibility(chatList.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(chatList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onChatClick(Chat chat) {
        String adminId = getAdminUserId();
        String otherUserId = findOtherUserId(chat, adminId);

        if (otherUserId == null || studentMap.get(otherUserId) == null) {
            Toast.makeText(this, "Error: User is not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (chat.getChatId() != null && chat.getLastSenderId() != null
                && !chat.getLastSenderId().equals(adminId) && !chat.isRead()) {
            db.collection("chats").document(chat.getChatId()).update("read", true);
        }

        Intent intent = new Intent(this, ChatWindowActivity.class);
        intent.putExtra("ADMIN_USER_ID", adminId);
        intent.putExtra(ChatWindowActivity.EXTRA_OTHER_USER_ID, otherUserId);
        startActivity(intent);
    }
}