package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.util.Log;

public class ChatFragment extends Fragment implements ChatListAdapter.OnChatSelectedListener {

    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private List<Chat> chatList;
    private Map<String, Student> studentMap;
    private FirebaseFirestore db;
    private String currentUserId;
    private ListenerRegistration chatListListener;
    private LinearLayout emptyView;
    private List<String> blockedUsersList = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        chatList = new ArrayList<>();
        studentMap = new HashMap<>();
        setupRecyclerView();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCurrentUserProfile();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (chatListListener != null) {
            chatListListener.remove();
        }
    }

    private void setupRecyclerView() {
        adapter = new ChatListAdapter(chatList, studentMap, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadCurrentUserProfile() {
        if (currentUserId == null) {
            loadAllUsersAndListenForChats();
            return;
        }
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Student currentUser = documentSnapshot.toObject(Student.class);
                        if (currentUser != null && currentUser.getBlockedUsers() != null) {
                            blockedUsersList = currentUser.getBlockedUsers();
                        }
                    }
                    loadAllUsersAndListenForChats();
                })
                .addOnFailureListener(e -> {
                    loadAllUsersAndListenForChats();
                });
    }

    private void loadAllUsersAndListenForChats() {
        if (currentUserId == null) return;

        db.collection("users").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    studentMap.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Student student = doc.toObject(Student.class);
                        studentMap.put(doc.getId(), student);
                    }

                    Student adminUser = new Student();
                    adminUser.setNickname("System Admin");
                    adminUser.setAvatar("🛡️");
                    adminUser.setRole("admin");
                    // Assuming ReportsManagementActivity.ADMIN_USER_ID is a defined constant
                    studentMap.put(ReportsManagementActivity.ADMIN_USER_ID, adminUser);

                    listenForChats();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error loading user data.", Toast.LENGTH_SHORT).show());
    }

    private void listenForChats() {
        if (currentUserId == null) return;

        CollectionReference chatsRef = db.collection("chats");

        if (chatListListener != null) chatListListener.remove();

        chatListListener = chatsRef
                // 1. Query: Fetch ALL chats where the current user is in the 'users' list.
                .whereArrayContains("users", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) {
                        Log.e("ChatFragment", "Listen failed: " + e.getMessage());
                        return;
                    }

                    chatList.clear();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Chat chat = doc.toObject(Chat.class);

                        String otherUserId = null;
                        if (chat.getUsers() != null) {
                            for (String id : chat.getUsers()) {
                                if (!id.equals(currentUserId)) {
                                    otherUserId = id;
                                    break;
                                }
                            }
                        }

                        // 2. Client-Side Filter: Check for soft deletion status using the Map
                        // If the 'deletedAt' map contains the currentUserId as a key, the chat is hidden.
                        boolean isSoftDeletedByMe = chat.getDeletedAt() != null
                                && chat.getDeletedAt().containsKey(currentUserId);

                        // 3. Final Check and Add
                        if (otherUserId != null
                                && studentMap.containsKey(otherUserId)
                                && !blockedUsersList.contains(otherUserId)
                                && !isSoftDeletedByMe) { // <-- Only add if NOT soft-deleted

                            // Set the Chat ID for the Chat object, as it's excluded during toObject()
                            chat.setChatId(doc.getId());
                            chatList.add(chat);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    checkIfEmpty();
                });
    }

    private void checkIfEmpty() {
        if (recyclerView == null || emptyView == null) return;
        if (chatList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onChatSelected(String otherUserId) {
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), ChatWindowActivity.class);
        intent.putExtra(ChatWindowActivity.EXTRA_OTHER_USER_ID, otherUserId);
        startActivity(intent);
    }
}