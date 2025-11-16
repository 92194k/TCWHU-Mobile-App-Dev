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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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

    private LinearLayout inputContainer;
    private LinearLayout adminWarningActions;
    private Button buttonConfirmWarning, buttonAppealWarning;

    private String currentUserId;
    private String otherUserId;
    private String chatId;
    private FirebaseFirestore db;
    private ListenerRegistration chatListener;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private boolean isChatWithAdmin = false;
    private boolean amIStudent = true;

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

        String adminIdFromIntent = getIntent().getStringExtra("ADMIN_USER_ID");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (adminIdFromIntent != null && !adminIdFromIntent.isEmpty()) {
            amIStudent = false;
            isChatWithAdmin = true;
            currentUserId = adminIdFromIntent;
            otherUserId = getIntent().getStringExtra(EXTRA_OTHER_USER_ID);
        } else {
            amIStudent = true;
            if (currentUser == null) {
                Toast.makeText(this, "Error: You are not logged in.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            currentUserId = currentUser.getUid();
            otherUserId = getIntent().getStringExtra(EXTRA_OTHER_USER_ID);
            isChatWithAdmin = otherUserId.equals(ReportsManagementActivity.ADMIN_USER_ID);
        }

        if (otherUserId == null || otherUserId.isEmpty()) {
            Toast.makeText(this, "Error: Chat partner is missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

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
        chatProgressBar = findViewById(R.id.chatProgressBar);
        inputContainer = findViewById(R.id.inputContainer);
        adminWarningActions = findViewById(R.id.adminWarningActions);
        buttonConfirmWarning = findViewById(R.id.buttonConfirmWarning);
        buttonAppealWarning = findViewById(R.id.buttonAppealWarning);

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

        // --- CRITICAL FIX: Mark chat as read for EVERYONE (student OR admin) ---
        // The markChatAsRead() method already has the logic to check
        // *who* sent the last message, so this is safe to call.
        markChatAsRead();
        // --- END OF FIX ---

        setupAdminWarningUI();

        buttonConfirmWarning.setOnClickListener(v -> {
            sendConfirmationMessage();
            adminWarningActions.setVisibility(View.GONE);
        });

        buttonAppealWarning.setOnClickListener(v -> {
            showAppealDialog();
        });

        buttonSend.setOnClickListener(v -> sendMessage());
        setupActionButtons();

        String warningTemplate = getIntent().getStringExtra("WARNING_TEMPLATE");
        if (warningTemplate != null && !warningTemplate.isEmpty()) {
            inputMessage.setText(warningTemplate);
        }

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

    private void setupAdminWarningUI() {
        if (isChatWithAdmin && amIStudent) {
            // I am a student talking to the admin
            inputContainer.setVisibility(View.GONE);

            // Check if warning has already been acknowledged
            db.collection("chats").document(chatId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && doc.getBoolean("warningAcknowledged") != null && doc.getBoolean("warningAcknowledged")) {
                            // Already acknowledged, hide the buttons
                            adminWarningActions.setVisibility(View.GONE);
                        } else {
                            // Not acknowledged, show the buttons
                            adminWarningActions.setVisibility(View.VISIBLE);
                        }
                    });

        } else if (isChatWithAdmin && !amIStudent) {
            // I am an admin talking to a student
            inputContainer.setVisibility(View.VISIBLE);
            adminWarningActions.setVisibility(View.GONE);
        } else {
            // Student-to-student chat
            inputContainer.setVisibility(View.VISIBLE);
            adminWarningActions.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!isChatWithAdmin) {
            getMenuInflater().inflate(R.menu.menu_chat_options, menu);
        }
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

                        if (year != null && !year.isEmpty() && !"admin".equals(documentSnapshot.getString("role"))) {
                            textChatYear.setText(year);
                            textChatYear.setVisibility(View.VISIBLE);
                        } else {
                            textChatYear.setVisibility(View.GONE);
                        }

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

                            if (!message.isSeen() && !message.getSenderId().equals(currentUserId)) {
                                markMessageAsSeen(dc.getDocument().getReference());
                            }
                        } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
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
            updateChatOverview(content, false); // Not an acknowledgment
        });
    }

    // --- UPDATED: This now takes a boolean ---
    private void updateChatOverview(String lastMessage, boolean isAcknowledgment) {
        Map<String, Object> chatOverview = new HashMap<>();
        chatOverview.put("lastMessage", lastMessage);
        chatOverview.put("timestamp", System.currentTimeMillis());
        chatOverview.put("users", Arrays.asList(currentUserId, otherUserId));
        chatOverview.put("lastSenderId", currentUserId);
        chatOverview.put("read", false);

        // --- CRITICAL FIX: This saves the student's choice ---
        if(isAcknowledgment) {
            chatOverview.put("warningAcknowledged", true);
        }

        db.collection("chats").document(chatId)
                .set(chatOverview) // Use set() to create or overwrite
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
        return -1;
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
            updateChatOverview("[Image]", false);
        });
    }

    private void saveMessageToFirestore(String type, String content, Runnable onSuccess) {
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUserId);
        message.put("content", content);
        message.put("type", type);
        message.put("timestamp", System.currentTimeMillis());
        message.put("seen", false);

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

    private void showAppealDialog() {
        final EditText inputAppeal = new EditText(this);
        inputAppeal.setHint("Please explain your side...");
        inputAppeal.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(this)
                .setTitle("Appeal Warning")
                .setMessage("Your appeal will be sent to the admin for review. Please be respectful.")
                .setView(inputAppeal)
                .setPositiveButton("Send Appeal", (dialog, which) -> {
                    String appealText = inputAppeal.getText().toString().trim();
                    if (!appealText.isEmpty()) {
                        String finalMessage = "[APPEAL]: " + appealText;
                        saveMessageToFirestore("text", finalMessage, () -> {
                            updateChatOverview(finalMessage, true); // This IS an acknowledgment
                        });
                        Toast.makeText(this, "Appeal sent.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "An explanation is required to appeal.", Toast.LENGTH_SHORT).show();
                        adminWarningActions.setVisibility(View.VISIBLE); // Show buttons again
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    adminWarningActions.setVisibility(View.VISIBLE); // Show buttons again
                })
                .show();
    }

    private void sendConfirmationMessage() {
        String message = "[User has read and acknowledged the warning]";
        saveMessageToFirestore("text", message, () -> {
            updateChatOverview(message, true); // This IS an acknowledgment
            Toast.makeText(this, "Warning Acknowledged", Toast.LENGTH_SHORT).show();
        });
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