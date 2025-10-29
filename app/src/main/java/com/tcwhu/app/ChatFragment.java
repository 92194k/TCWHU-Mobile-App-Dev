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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatFragment extends Fragment implements ChatListAdapter.OnChatSelectedListener {

    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private List<Chat> chatList;
    private Map<String, Student> studentMap;
    private FirebaseFirestore db;
    private String currentUserId;
    private ListenerRegistration chatListListener;
    private LinearLayout emptyView;

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
        loadAllUsersAndListenForChats();
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

    private void loadAllUsersAndListenForChats() {
        if (currentUserId == null) return;

        db.collection("users").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    studentMap.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Student student = doc.toObject(Student.class);
                        studentMap.put(doc.getId(), student);
                    }
                    listenForChats();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load user data for chat.", Toast.LENGTH_SHORT).show());
    }

    private void listenForChats() {
        if (currentUserId == null) return;

        CollectionReference chatsRef = db.collection("chats");

        // CRITICAL FIX: Query the correct field name
        chatListListener = chatsRef
                .whereArrayContains("users", currentUserId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(getContext(), "Error loading chats.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots != null) {
                        chatList.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Chat chat = doc.toObject(Chat.class);
                            chatList.add(chat);
                        }
                        adapter.notifyDataSetChanged();
                        checkIfEmpty();
                    }
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