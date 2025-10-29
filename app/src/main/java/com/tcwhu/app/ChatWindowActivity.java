package com.tcwhu.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatWindowActivity extends AppCompatActivity {

    public static final String EXTRA_OTHER_USER_ID = "otherUserId";
    private RecyclerView messagesRecyclerView;
    private MessagesAdapter messagesAdapter;
    private List<Message> messageList;
    private EditText inputMessage;
    private ImageButton buttonSend;
    private TextView textChatUsername, textChatYear;
    private String currentUserId;
    private String otherUserId;
    private String chatId;
    private FirebaseFirestore db;
    private ListenerRegistration chatListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_window);

        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        otherUserId = getIntent().getStringExtra(EXTRA_OTHER_USER_ID);

        if (currentUser == null || otherUserId == null || otherUserId.isEmpty()) {
            Toast.makeText(this, "Error: Chat session is invalid.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        currentUserId = currentUser.getUid();

        chatId = (currentUserId.compareTo(otherUserId) < 0) ? currentUserId + "_" + otherUserId : otherUserId + "_" + currentUserId;

        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        inputMessage = findViewById(R.id.inputMessage);
        buttonSend = findViewById(R.id.buttonSend);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        textChatUsername = findViewById(R.id.textChatUsername);
        textChatYear = findViewById(R.id.textChatYear);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        messageList = new ArrayList<>();
        messagesAdapter = new MessagesAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messagesRecyclerView.setLayoutManager(layoutManager);
        messagesRecyclerView.setAdapter(messagesAdapter);
        loadChatPartnerDetails();
        listenForMessages();
        buttonSend.setOnClickListener(v -> sendMessage());
        setupMockActionButtons();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (chatListener != null) {
            chatListener.remove();
        }
    }

    private void loadChatPartnerDetails() {
        db.collection("users").document(otherUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nickname = documentSnapshot.getString("nickname");
                        String year = documentSnapshot.getString("yearLevel");
                        textChatUsername.setText(nickname);
                        textChatYear.setText(year);
                    }
                });
    }

    private void listenForMessages() {
        CollectionReference messagesRef = db.collection("chats").document(chatId).collection("messages");
        chatListener = messagesRef.orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) { return; }
                    if (snapshots != null) {
                        messageList.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Message message = doc.toObject(Message.class);
                            messageList.add(message);
                        }
                        messagesAdapter.notifyDataSetChanged();
                        messagesRecyclerView.scrollToPosition(messageList.size() - 1);
                    }
                });
    }

    private void sendMessage() {
        String content = inputMessage.getText().toString().trim();
        if (TextUtils.isEmpty(content)) { return; }
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUserId);
        message.put("content", content);
        message.put("type", "text");
        message.put("timestamp", System.currentTimeMillis());
        db.collection("chats").document(chatId).collection("messages").add(message)
                .addOnSuccessListener(documentReference -> {
                    inputMessage.setText("");
                    updateChatOverview(content);
                });
    }

    private void updateChatOverview(String lastMessage) {
        Map<String, Object> chatOverview = new HashMap<>();
        chatOverview.put("lastMessage", lastMessage);
        chatOverview.put("timestamp", System.currentTimeMillis());
        chatOverview.put("users", Arrays.asList(currentUserId, otherUserId)); // CORRECTED field name

        db.collection("chats").document(chatId).set(chatOverview);
    }

    private void setupMockActionButtons() {
        ImageButton buttonPaperclip = findViewById(R.id.buttonPaperclip);
        ImageButton buttonMic = findViewById(R.id.buttonMic);
        buttonPaperclip.setOnClickListener(v -> Toast.makeText(this, "File sharing disabled for safety.", Toast.LENGTH_SHORT).show());
        buttonMic.setOnClickListener(v -> Toast.makeText(this, "Voice recording disabled.", Toast.LENGTH_SHORT).show());
    }
}