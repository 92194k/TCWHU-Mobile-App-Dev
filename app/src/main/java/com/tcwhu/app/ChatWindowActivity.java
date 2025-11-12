package com.tcwhu.app;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChatWindowActivity extends AppCompatActivity {

    public static final String EXTRA_OTHER_USER_ID = "otherUserId";
    private static final long MAX_FILE_SIZE_MB = 25;
    private static final long MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024;

    private RecyclerView messagesRecyclerView;
    private MessagesAdapter messagesAdapter;
    private List<Message> messageList;
    private EditText inputMessage;
    private ImageButton buttonSend, buttonPaperclip, buttonMic;
    private TextView textChatUsername, textChatYear, textChattingWithBanner;
    private ProgressBar chatProgressBar;
    private String currentUserId;
    private String otherUserId;
    private String chatId;
    private FirebaseFirestore db;
    private ListenerRegistration chatListener;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_window);

        // Initialize Cloudinary
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dggeonpfw");
            config.put("api_key", "147481881754886");
            config.put("api_secret", "583Dz7vp2y6TRaDBuCj8HbHoQX4");
            MediaManager.init(this, config);
        } catch (IllegalStateException e) {
            Log.i("ChatWindowActivity", "Cloudinary already initialized.");
        }

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
        textChattingWithBanner = findViewById(R.id.textChattingWithBanner);
        buttonPaperclip = findViewById(R.id.buttonPaperclip);
        buttonMic = findViewById(R.id.buttonMic);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        messageList = new ArrayList<>();
        messagesAdapter = new MessagesAdapter(messageList, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messagesRecyclerView.setLayoutManager(layoutManager);
        messagesRecyclerView.setAdapter(messagesAdapter);

        loadChatPartnerDetails();
        listenForMessages();
        markChatAsRead(); // --- ADDED: Mark as read when opening ---

        buttonSend.setOnClickListener(v -> sendMessage());
        setupActionButtons();

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            if (getFileSize(selectedImageUri) > MAX_FILE_SIZE_BYTES) {
                                Toast.makeText(this, "File is too large. Max size is " + MAX_FILE_SIZE_MB + "MB.", Toast.LENGTH_LONG).show();
                            } else {
                                uploadImageToCloudinary(selectedImageUri);
                            }
                        }
                    }
                }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_report) {
            showReportDialog();
            return true;
        } else if (itemId == R.id.menu_block) {
            showBlockDialog();
            return true;
        } else if (itemId == R.id.menu_delete_conversation) {
            showDeleteDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (chatListener != null) chatListener.remove();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatListener != null) {
            chatListener.remove();
            chatListener = null;
        }
    }

    private void loadChatPartnerDetails() {
        db.collection("users").document(otherUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nickname = documentSnapshot.getString("nickname");
                        String year = documentSnapshot.getString("yearLevel");

                        textChatUsername.setText(nickname != null ? nickname : "Unknown");
                        textChatYear.setText(year != null ? year : "");
                        textChattingWithBanner.setText("You are chatting with " + (nickname != null ? nickname : "Unknown"));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load user details.", Toast.LENGTH_SHORT).show());
    }

    private void listenForMessages() {
        CollectionReference messagesRef = db.collection("chats")
                .document(chatId)
                .collection("messages");

        chatListener = messagesRef
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    boolean isAtBottom = !messagesRecyclerView.canScrollVertically(1);
                    List<Message> newMessages = new ArrayList<>();

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            Message message = dc.getDocument().toObject(Message.class);
                            newMessages.add(message);

                            // Mark message as seen by this user
                            if (!message.isSeen() && !message.getSenderId().equals(currentUserId)) {
                                markMessageAsSeen(dc.getDocument().getReference());
                            }
                        } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
                            // This handles the "seen" status update in real-time
                            Message modifiedMessage = dc.getDocument().toObject(Message.class);
                            for(int i = 0; i < messageList.size(); i++) {
                                if (messageList.get(i).getTimestamp() == modifiedMessage.getTimestamp()) {
                                    messageList.set(i, modifiedMessage);
                                    messagesAdapter.notifyItemChanged(i);
                                    break;
                                }
                            }
                        }
                    }

                    int oldSize = messageList.size();
                    messageList.addAll(newMessages);
                    messagesAdapter.notifyItemRangeInserted(oldSize, newMessages.size());

                    if (isAtBottom) {
                        messagesRecyclerView.scrollToPosition(Math.max(0, messageList.size() - 1));
                    }
                });
    }

    private void markMessageAsSeen(DocumentReference messageRef) {
        messageRef.update("seen", true);
    }

    // --- ADDED: Method to mark the chat summary as "read" ---
    private void markChatAsRead() {
        db.collection("chats").document(chatId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Chat chat = documentSnapshot.toObject(Chat.class);
                        // Only update if the last message was NOT from me
                        if (chat != null && !currentUserId.equals(chat.getLastSenderId())) {
                            documentSnapshot.getReference().update("read", true);
                        }
                    }
                });
    }

    private void sendMessage() {
        String content = inputMessage.getText().toString().trim();
        if (TextUtils.isEmpty(content)) return;

        saveMessageToFirestore("text", content, () -> {
            inputMessage.setText("");
            updateChatOverview(content); // Update summary AFTER message is sent
        });
    }

    private void updateChatOverview(String lastMessage) {
        Map<String, Object> chatOverview = new HashMap<>();
        chatOverview.put("lastMessage", lastMessage);
        chatOverview.put("timestamp", System.currentTimeMillis());
        // --- CRITICAL FIX: Use "users" to match your ChatFragment query ---
        chatOverview.put("users", Arrays.asList(currentUserId, otherUserId));
        chatOverview.put("lastSenderId", currentUserId); // <-- ADDED
        chatOverview.put("read", false); // <-- ADDED: Mark as unread for the other user

        db.collection("chats").document(chatId)
                .set(chatOverview)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update chat overview.", Toast.LENGTH_SHORT).show());
    }

    private void setupActionButtons() {
        buttonPaperclip.setOnClickListener(v -> openImagePicker());
        buttonMic.setOnClickListener(v ->
                Toast.makeText(this, "Voice recording disabled.", Toast.LENGTH_SHORT).show());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private long getFileSize(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (!cursor.isNull(sizeIndex)) {
                    return cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception e) {
            Log.e("ChatWindow", "Error getting file size", e);
        }
        return -1; // Error
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        chatProgressBar.setVisibility(View.VISIBLE);
        String publicId = "chat_images/" + chatId + "/" + UUID.randomUUID().toString();

        MediaManager.get().upload(imageUri)
                .option("public_id", publicId)
                .callback(new UploadCallback() {
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        chatProgressBar.setVisibility(View.GONE);
                        String url = (String) resultData.get("secure_url");
                        sendImageMessage(url);
                    }
                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        chatProgressBar.setVisibility(View.GONE);
                        Toast.makeText(ChatWindowActivity.this, "Image upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void sendImageMessage(String imageUrl) {
        saveMessageToFirestore("image", imageUrl, () -> {
            updateChatOverview("[Image]");
        });
    }

    private void saveMessageToFirestore(String type, String content, Runnable onSuccess) {
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUserId);
        message.put("content", content);
        message.put("type", type);
        message.put("timestamp", System.currentTimeMillis());
        message.put("seen", false); // All messages start as "not seen"

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send message.", Toast.LENGTH_SHORT).show());
    }

    private void showReportDialog() {
        final EditText inputReason = new EditText(this);
        inputReason.setHint("Please provide a reason...");
        inputReason.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(this)
                .setTitle("Report User")
                .setMessage("This report will be sent to an administrator.")
                .setView(inputReason)
                .setPositiveButton("Submit Report", (dialog, which) -> {
                    String reason = inputReason.getText().toString().trim();
                    if (!reason.isEmpty()) {
                        reportUser(reason);
                    } else {
                        Toast.makeText(this, "A reason is required to submit a report.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showBlockDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Block User")
                .setMessage("Are you sure you want to block this user?\n\n- You will no longer see them.\n- Your conversation will be hidden.\n\n⚠️ THIS ACTION CANNOT BE REVERTED.")
                .setPositiveButton("Block", (dialog, which) -> blockUser())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Conversation")
                .setMessage("Are you sure you want to permanently delete all messages in this conversation?\n\n⚠️ THIS ACTION CANNOT BE REVERTED.")
                .setPositiveButton("Delete", (dialog, which) -> deleteConversation())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void reportUser(String reason) {
        Report report = new Report(currentUserId, otherUserId, reason);
        db.collection("reports").add(report)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(this, "User reported. An admin will review it.", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send report. Try again later.", Toast.LENGTH_SHORT).show());
    }

    private void blockUser() {
        db.collection("users").document(currentUserId)
                .update("blockedUsers", FieldValue.arrayUnion(otherUserId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "User has been blocked.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to block user.", Toast.LENGTH_SHORT).show());
    }

    private void deleteConversation() {
        CollectionReference messagesRef = db.collection("chats").document(chatId).collection("messages");

        messagesRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                WriteBatch batch = db.batch();

                for (QueryDocumentSnapshot doc : task.getResult()) {
                    batch.delete(doc.getReference());
                }

                batch.delete(db.collection("chats").document(chatId));

                batch.commit()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(ChatWindowActivity.this, "Conversation permanently deleted.", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Failed to delete conversation.", Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "Error finding messages to delete.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}