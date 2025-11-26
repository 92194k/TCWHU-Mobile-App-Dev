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
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// NOTE: Placeholder classes (Message, Chat, Report, ReportsManagementActivity) are assumed to exist.

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

        // Detect admin mode
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

            // Assuming ReportsManagementActivity.ADMIN_USER_ID is defined elsewhere
            // and the ReportsManagementActivity class is available in the package.
            // If ReportsManagementActivity is not available, replace this check with the hardcoded admin ID if known.
            isChatWithAdmin = otherUserId != null && otherUserId.equals(ReportsManagementActivity.ADMIN_USER_ID);
        }

        if (otherUserId == null || otherUserId.isEmpty()) {
            Toast.makeText(this, "Error: Chat partner missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        chatId = (currentUserId.compareTo(otherUserId) < 0)
                ? currentUserId + "_" + otherUserId
                : otherUserId + "_" + currentUserId;

        // UI initialization
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        inputMessage = findViewById(R.id.inputMessage);
        buttonSend = findViewById(R.id.buttonSend);
        buttonPaperclip = findViewById(R.id.buttonPaperclip);
        buttonMic = findViewById(R.id.buttonMic);
        chatProgressBar = findViewById(R.id.chatProgressBar);

        textChatUsername = findViewById(R.id.textChatUsername);
        textChatYear = findViewById(R.id.textChatYear);
        textChattingWithBanner = findViewById(R.id.textChattingWithBanner);

        inputContainer = findViewById(R.id.inputContainer);
        adminWarningActions = findViewById(R.id.adminWarningActions);
        buttonConfirmWarning = findViewById(R.id.buttonConfirmWarning);
        buttonAppealWarning = findViewById(R.id.buttonAppealWarning);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        messageList = new ArrayList<>();
        // Using the updated MessagesAdapter constructor that takes ChatWindowActivity
        messagesAdapter = new MessagesAdapter(messageList, currentUserId, this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messagesRecyclerView.setLayoutManager(layoutManager);
        messagesRecyclerView.setAdapter(messagesAdapter);

        loadChatPartnerDetails();
        listenForMessages();
        markChatAsRead();

        setupAdminWarningUI();

        buttonConfirmWarning.setOnClickListener(v -> {
            sendConfirmationMessage();
            adminWarningActions.setVisibility(View.GONE);
        });

        buttonAppealWarning.setOnClickListener(v -> showAppealDialog());

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
                                Toast.makeText(this, "File too large. Max is " + MAX_FILE_SIZE_MB + "MB", Toast.LENGTH_LONG).show();
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
            inputContainer.setVisibility(View.GONE);

            db.collection("chats").document(chatId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() &&
                                doc.getBoolean("warningAcknowledged") != null &&
                                doc.getBoolean("warningAcknowledged")) {
                            adminWarningActions.setVisibility(View.GONE);
                        } else {
                            adminWarningActions.setVisibility(View.VISIBLE);
                        }
                    });

        } else {
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
        int id = item.getItemId();

        if (id == R.id.menu_report) {
            showReportDialog();
            return true;
        } else if (id == R.id.menu_block) {
            showBlockDialog();
            return true;
        } else if (id == R.id.menu_delete_conversation) {
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
        if (chatListener != null) chatListener.remove();
        if (messagesAdapter != null) messagesAdapter.cleanup(); // Clean up MediaPlayer
    }

    private void loadChatPartnerDetails() {
        db.collection("users").document(otherUserId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String nickname = doc.getString("nickname");
                        String year = doc.getString("yearLevel");

                        textChatUsername.setText(nickname != null ? nickname : "Unknown");

                        if (year != null && !"admin".equals(doc.getString("role"))) {
                            textChatYear.setText(year);
                            textChatYear.setVisibility(View.VISIBLE);
                        } else {
                            textChatYear.setVisibility(View.GONE);
                        }

                        textChattingWithBanner.setText("You are chatting with " + (nickname != null ? nickname : "Unknown"));
                    }
                });
    }

    private void listenForMessages() {
        CollectionReference messagesRef = db.collection("chats")
                .document(chatId).collection("messages");

        chatListener = messagesRef
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    boolean atBottom = !messagesRecyclerView.canScrollVertically(1);

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {

                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            Message m = dc.getDocument().toObject(Message.class);
                            messageList.add(m);

                            if (!m.isSeen() && !m.getSenderId().equals(currentUserId)) {
                                markMessageAsSeen(dc.getDocument().getReference());
                            }

                        } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
                            Message m = dc.getDocument().toObject(Message.class);
                            for (int i = 0; i < messageList.size(); i++) {
                                if (messageList.get(i).getTimestamp() == m.getTimestamp()) {
                                    messageList.set(i, m);
                                    messagesAdapter.notifyItemChanged(i);
                                    break;
                                }
                            }
                        } else if (dc.getType() == DocumentChange.Type.REMOVED) {
                            // Find and remove the deleted message
                            Message m = dc.getDocument().toObject(Message.class);
                            for (int i = 0; i < messageList.size(); i++) {
                                if (messageList.get(i).getTimestamp() == m.getTimestamp()) {
                                    messageList.remove(i);
                                    messagesAdapter.notifyItemRemoved(i);
                                    break;
                                }
                            }
                        }
                    }

                    messagesAdapter.notifyDataSetChanged();

                    if (atBottom && messageList.size() > 0) {
                        messagesRecyclerView.scrollToPosition(messageList.size() - 1);
                    }
                });
    }

    private void markMessageAsSeen(DocumentReference ref) {
        ref.update("seen", true);
    }

    private void markChatAsRead() {
        db.collection("chats").document(chatId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Chat chat = doc.toObject(Chat.class);
                        if (chat != null &&
                                chat.getLastSenderId() != null &&
                                !chat.getLastSenderId().equals(currentUserId)) {
                            doc.getReference().update("read", true);
                        }
                    }
                });
    }

    private void sendMessage() {
        String text = inputMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        saveMessageToFirestore("text", text, () -> {
            inputMessage.setText("");
            updateChatOverview(text, false);
        });
    }

    private void updateChatOverview(String lastMessage, boolean isAcknowledgment) {

        Map<String, Object> chatData = new HashMap<>();
        chatData.put("lastMessage", lastMessage);
        chatData.put("timestamp", System.currentTimeMillis());
        chatData.put("users", Arrays.asList(currentUserId, otherUserId));
        chatData.put("lastSenderId", currentUserId);
        chatData.put("read", false);

        if (isAcknowledgment) {
            chatData.put("warningAcknowledged", true);
        }

        db.collection("chats").document(chatId).set(chatData);
    }

    private void setupActionButtons() {
        buttonPaperclip.setOnClickListener(v -> openImagePicker());
        buttonMic.setOnClickListener(v ->
                Toast.makeText(this, "Voice recording disabled.", Toast.LENGTH_SHORT).show()
        );
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private long getFileSize(Uri uri) {
        Cursor cursor = null;
        try {
            ContentResolver contentResolver = getContentResolver();
            cursor = contentResolver.query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex != -1) {
                    return cursor.getLong(sizeIndex);
                } else {
                    Log.e("ChatWindow", "Could not find size column.");
                    return -1;
                }
            }
        } catch (Exception e) {
            Log.e("ChatWindow", "Error getting file size", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return -1;
    }

    private void uploadImageToCloudinary(Uri uri) {
        chatProgressBar.setVisibility(View.VISIBLE);

        String publicId = "chat_images/" + chatId + "/" + UUID.randomUUID();

        MediaManager.get().upload(uri)
                .option("public_id", publicId)
                .callback(new UploadCallback() {
                    @Override
                    public void onSuccess(String reqId, Map result) {
                        chatProgressBar.setVisibility(View.GONE);
                        String url = (String) result.get("secure_url");
                        sendImageMessage(url);
                    }

                    @Override
                    public void onError(String reqId, ErrorInfo error) {
                        chatProgressBar.setVisibility(View.GONE);
                        Toast.makeText(ChatWindowActivity.this,
                                "Upload failed: " + error.getDescription(),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override public void onStart(String r) {}
                    @Override public void onProgress(String r, long b, long t) {}
                    @Override public void onReschedule(String r, ErrorInfo e) {}
                }).dispatch();
    }

    private void sendImageMessage(String imageUrl) {
        saveMessageToFirestore("image", imageUrl,
                () -> updateChatOverview("[Image]", false));
    }

    private void saveMessageToFirestore(String type, String content, Runnable success) {

        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", currentUserId);
        msg.put("content", content);
        msg.put("type", type);
        msg.put("timestamp", System.currentTimeMillis());
        msg.put("seen", false);
        msg.put("status", 0); // 0: active, 1: deleted

        db.collection("chats").document(chatId)
                .collection("messages")
                .add(msg)
                .addOnSuccessListener(ref -> {
                    if (success != null) success.run();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send.", Toast.LENGTH_SHORT).show());
    }

    // --- Message Options Implementation (Fix for MessagesAdapter error) ---

    /**
     * Public method called by MessagesAdapter on long-press.
     * Shows context-specific options (Delete for sent, Report for received).
     * @param message The message that was long-pressed.
     */
    public void showMessageOptions(Message message) {
        if (message.getSenderId().equals(currentUserId)) {
            // Option for messages sent by the current user (Delete only)
            new AlertDialog.Builder(this)
                    .setTitle("Message Options")
                    .setItems(new String[]{"Delete Message"}, (dialog, which) -> {
                        if (which == 0) {
                            showDeleteMessageDialog(message);
                        }
                    })
                    .show();
        } else {
            // Option for messages sent by the other user (Report only)
            new AlertDialog.Builder(this)
                    .setTitle("Message Options")
                    .setItems(new String[]{"Report Message"}, (dialog, which) -> {
                        if (which == 0) {
                            showReportMessageDialog(message);
                        }
                    })
                    .show();
        }
    }

    private void showDeleteMessageDialog(Message message) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Message")
                .setMessage("Are you sure you want to delete this message?")
                .setPositiveButton("Delete", (d, w) -> deleteSingleMessage(message))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showReportMessageDialog(Message message) {
        EditText reasonInput = new EditText(this);
        reasonInput.setHint("Why are you reporting this message?");
        reasonInput.setPadding(40, 20, 40, 20);

        new AlertDialog.Builder(this)
                .setTitle("Report Message")
                .setMessage("Message content: \"" + message.getContent() + "\"\nThis report will be sent to an admin.")
                .setView(reasonInput)
                .setPositiveButton("Submit Report", (d, w) -> {
                    String reason = reasonInput.getText().toString().trim();
                    if (!reason.isEmpty()) {
                        reportMessage(message, reason);
                    } else {
                        Toast.makeText(this, "Reason required.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSingleMessage(Message message) {
        db.collection("chats").document(chatId)
                .collection("messages")
                // Find message by its unique timestamp and sender ID (assuming unique enough)
                .whereEqualTo("timestamp", message.getTimestamp())
                .whereEqualTo("senderId", message.getSenderId())
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Mark the message as deleted (status = 1) instead of fully deleting
                        // This prevents the message from being removed from the other user's view
                        // unless you intended a client-side deletion (which is usually a remove).
                        // For a simple visible deletion/placeholder, we update the status.

                        DocumentReference docRef = queryDocumentSnapshots.getDocuments().get(0).getReference();

                        // Option 1: Update status to 1 (Deleted placeholder)
                        docRef.update("status", 1)
                                .addOnSuccessListener(v -> Toast.makeText(this, "Message deleted (placeholder).", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update message status.", Toast.LENGTH_SHORT).show());

                        // NOTE: If you intended a full deletion, uncomment the lines below and comment out the update above:
                        /*
                        WriteBatch batch = db.batch();
                        batch.delete(docRef);
                        batch.commit()
                            .addOnSuccessListener(v -> Toast.makeText(this, "Message fully deleted.", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete message.", Toast.LENGTH_SHORT).show());
                        */

                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to find message for deletion.", Toast.LENGTH_SHORT).show());
    }

    private void reportMessage(Message message, String reason) {
        // Report specific message and sender to the admin
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("reporterId", currentUserId);
        reportData.put("reportedUserId", message.getSenderId());
        reportData.put("messageContent", message.getContent());
        reportData.put("messageTimestamp", message.getTimestamp());
        reportData.put("chatId", chatId);
        reportData.put("reason", reason);
        reportData.put("timestamp", System.currentTimeMillis());

        db.collection("message_reports").add(reportData)
                .addOnSuccessListener(r ->
                        Toast.makeText(this, "Message reported.", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send report.", Toast.LENGTH_SHORT).show());
    }
    // --- End Message Options ---


    private void showReportDialog() {
        EditText reasonInput = new EditText(this);
        reasonInput.setHint("Provide a reason...");
        reasonInput.setPadding(40, 20, 40, 20);

        new AlertDialog.Builder(this)
                .setTitle("Report User")
                .setMessage("This report will be sent to an admin.")
                .setView(reasonInput)
                .setPositiveButton("Submit", (d, w) -> {
                    String reason = reasonInput.getText().toString().trim();
                    if (!reason.isEmpty()) reportUser(reason);
                    else Toast.makeText(this, "Reason required.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAppealDialog() {
        EditText input = new EditText(this);
        input.setHint("Explain your side...");
        input.setPadding(40, 20, 40, 20);

        new AlertDialog.Builder(this)
                .setTitle("Appeal Warning")
                .setMessage("Your appeal will be reviewed by the admin.")
                .setView(input)
                .setPositiveButton("Send", (d, w) -> {
                    String appeal = input.getText().toString().trim();
                    if (!appeal.isEmpty()) {
                        String msg = "[APPEAL]: " + appeal;
                        saveMessageToFirestore("text", msg, () ->
                                updateChatOverview(msg, true)
                        );
                        Toast.makeText(this, "Appeal sent.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Explanation required.", Toast.LENGTH_SHORT).show();
                        adminWarningActions.setVisibility(View.VISIBLE);
                    }
                })
                .setNegativeButton("Cancel", (d, w) -> adminWarningActions.setVisibility(View.VISIBLE))
                .show();
    }

    private void sendConfirmationMessage() {
        String msg = "[User has read and acknowledged the warning]";

        saveMessageToFirestore("text", msg, () -> {
            updateChatOverview(msg, true);
            Toast.makeText(this, "Warning acknowledged.", Toast.LENGTH_SHORT).show();
        });
    }

    private void showBlockDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Block User")
                .setMessage("Blocking is permanent.\nYou will no longer see this user.")
                .setPositiveButton("Block", (d, w) -> blockUser())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Conversation")
                .setMessage("This cannot be undone.\nDelete all messages?")
                .setPositiveButton("Delete", (d, w) -> deleteConversation())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void reportUser(String reason) {
        Report report = new Report(currentUserId, otherUserId, reason);

        db.collection("reports").add(report)
                .addOnSuccessListener(r ->
                        Toast.makeText(this, "Reported.", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed.", Toast.LENGTH_SHORT).show());
    }

    private void blockUser() {
        db.collection("users").document(currentUserId)
                .update("blockedUsers", FieldValue.arrayUnion(otherUserId))
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "User blocked.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed.", Toast.LENGTH_SHORT).show());
    }

    private void deleteConversation() {
        CollectionReference messagesRef =
                db.collection("chats").document(chatId).collection("messages");

        messagesRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                WriteBatch batch = db.batch();

                for (QueryDocumentSnapshot doc : task.getResult()) {
                    batch.delete(doc.getReference());
                }

                batch.delete(db.collection("chats").document(chatId));

                batch.commit()
                        .addOnSuccessListener(v -> {
                            Toast.makeText(this, "Conversation deleted.", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Failed to delete.", Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "Failed to fetch messages for deletion.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}