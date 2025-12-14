package com.tcwhu.app;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import java.util.HashMap;
import java.util.Map;

public class ChatActionHandler implements MessageOptionHandler {
    private final Context context;
    private final String currentUserId;
    private final String otherUserId;
    private final String chatId;
    private final FirebaseFirestore db;
    private final ChatDataManager dataManager;
    private final ChatWindowCallbacks callbacks;
    private final ChatFileUploader fileUploader;

    public ChatActionHandler(Context context, String currentUserId, String otherUserId, String chatId,
                             ChatDataManager dataManager, ChatWindowCallbacks callbacks, ChatFileUploader fileUploader) {
        this.context = context;
        this.currentUserId = currentUserId;
        this.otherUserId = otherUserId;
        this.chatId = chatId;
        this.db = FirebaseFirestore.getInstance();
        this.dataManager = dataManager;
        this.callbacks = callbacks;
        this.fileUploader = fileUploader;
    }

    public void showReportDialog() {
        EditText reasonInput = new EditText(context);
        reasonInput.setHint("Provide a reason...");
        reasonInput.setPadding(40, 20, 40, 20);

        new AlertDialog.Builder(context)
                .setTitle("Report User")
                .setMessage("This report will be sent to an admin.")
                .setView(reasonInput)
                .setPositiveButton("Submit", (d, w) -> {
                    String reason = reasonInput.getText().toString().trim();
                    if (!reason.isEmpty()) reportUser(reason);
                    else callbacks.showToast("Reason required.", Toast.LENGTH_SHORT);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void showBlockDialog() {
        new AlertDialog.Builder(context)
                .setTitle("Block User")
                .setMessage("Blocking is permanent. You will no longer see this user.")
                .setPositiveButton("Block", (d, w) -> blockUser())
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void showDeleteDialog() {
        new AlertDialog.Builder(context)
                .setTitle("Delete Conversation")
                .setMessage("This will clear your view of the chat history.")
                .setPositiveButton("Delete", (d, w) -> dataManager.deleteConversation())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void reportUser(String reason) {
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("reporterId", currentUserId);
        reportData.put("reportedUserId", otherUserId);
        reportData.put("chatId", chatId);
        reportData.put("reason", reason);
        reportData.put("type", "USER_REPORT");
        reportData.put("timestamp", System.currentTimeMillis());
        reportData.put("status", "pending");
        reportData.put("resolved", false);

        db.collection("reports").add(reportData)
                .addOnSuccessListener(r -> callbacks.showToast("User reported successfully.", Toast.LENGTH_LONG))
                .addOnFailureListener(e -> callbacks.showToast("Failed to report user.", Toast.LENGTH_SHORT));
    }

    private void blockUser() {
        db.collection("users").document(currentUserId)
                .update("blockedUsers", FieldValue.arrayUnion(otherUserId))
                .addOnSuccessListener(v -> {
                    callbacks.showToast("User blocked.", Toast.LENGTH_SHORT);
                    callbacks.finishActivity();
                })
                .addOnFailureListener(e -> callbacks.showToast("Failed to block.", Toast.LENGTH_SHORT));
    }

    @Override
    public void showMessageOptions(Message message) {
        if (message.getStatus() == 0 && message.getSenderId().equals(currentUserId)) {
            new AlertDialog.Builder(context)
                    .setTitle("Message Options")
                    .setItems(new String[]{"Delete Message"}, (dialog, which) -> {
                        if (which == 0) showDeleteMessageDialog(message);
                    })
                    .show();
        } else if (message.getStatus() == 0) {
            new AlertDialog.Builder(context)
                    .setTitle("Message Options")
                    .setItems(new String[]{"Report Message"}, (dialog, which) -> {
                        if (which == 0) showReportMessageDialog(message);
                    })
                    .show();
        }
    }

    private void showDeleteMessageDialog(Message message) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Message")
                .setMessage("Are you sure you want to delete this message? This action is permanent.")
                .setPositiveButton("Delete", (d, w) -> deleteSingleMessage(message))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showReportMessageDialog(Message message) {
        EditText reasonInput = new EditText(context);
        reasonInput.setHint("Why are you reporting this message?");
        reasonInput.setPadding(40, 20, 40, 20);

        new AlertDialog.Builder(context)
                .setTitle("Report Message")
                .setMessage("Message content: \"" + message.getContent() + "\"")
                .setView(reasonInput)
                .setPositiveButton("Submit Report", (d, w) -> {
                    String reason = reasonInput.getText().toString().trim();
                    if (!reason.isEmpty()) reportMessage(message, reason);
                    else callbacks.showToast("Reason required.", Toast.LENGTH_SHORT);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSingleMessage(Message message) {
        DocumentReference messageRef = db.collection("chats").document(chatId)
                .collection("messages").document(message.getMessageId());

        callbacks.onProgressVisibilityChanged(View.VISIBLE);

        // Delete associated media if present, then update Firestore
        if (message.getType() != null && !message.getType().equals("text") && message.getContent() != null && fileUploader != null) {
            fileUploader.deleteCloudinaryFile(message.getContent(), () -> updateFirestoreMessage(messageRef));
        } else {
            updateFirestoreMessage(messageRef);
        }
    }

    private void updateFirestoreMessage(DocumentReference messageRef) {
        WriteBatch batch = db.batch();
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", 1); // 1 = Deleted
        updates.put("content", "[This message was deleted]");
        updates.put("type", "text");
        updates.put("fileName", FieldValue.delete());
        updates.put("mediaDuration", FieldValue.delete());

        batch.update(messageRef, updates);

        batch.commit()
                .addOnSuccessListener(v -> {
                    callbacks.onProgressVisibilityChanged(View.GONE);
                    callbacks.showToast("Message deleted.", Toast.LENGTH_SHORT);
                })
                .addOnFailureListener(e -> {
                    callbacks.onProgressVisibilityChanged(View.GONE);
                    callbacks.showToast("Failed to delete message.", Toast.LENGTH_SHORT);
                    Log.e("ChatHandler", "Firestore message delete failed: ", e);
                });
    }

    private void reportMessage(Message message, String reason) {
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("reporterId", currentUserId);
        reportData.put("reportedUserId", message.getSenderId());
        reportData.put("messageContent", message.getContent());
        reportData.put("messageTimestamp", message.getTimestamp());
        reportData.put("chatId", chatId);
        reportData.put("reason", reason);
        reportData.put("type", "MESSAGE_REPORT");
        reportData.put("timestamp", System.currentTimeMillis());
        reportData.put("status", "pending");
        reportData.put("resolved", false);

        db.collection("reports").add(reportData)
                .addOnSuccessListener(r -> callbacks.showToast("Message reported.", Toast.LENGTH_LONG))
                .addOnFailureListener(e -> callbacks.showToast("Failed to send report.", Toast.LENGTH_SHORT));
    }

    public void showAppealDialog(View adminWarningActions) {
        EditText input = new EditText(context);
        input.setHint("Explain your side...");
        input.setPadding(40, 20, 40, 20);

        new AlertDialog.Builder(context)
                .setTitle("Appeal Warning")
                .setMessage("Your appeal will be reviewed by the admin.")
                .setView(input)
                .setPositiveButton("Send", (d, w) -> {
                    String appeal = input.getText().toString().trim();
                    if (!appeal.isEmpty()) {
                        String msg = "[APPEAL]: " + appeal;
                        dataManager.saveMessageToFirestore("text", msg, null, 0L, () ->
                                dataManager.updateChatOverview(msg, true)
                        );
                        callbacks.showToast("Appeal sent.", Toast.LENGTH_SHORT);
                        adminWarningActions.setVisibility(View.GONE);
                    } else {
                        callbacks.showToast("Explanation required.", Toast.LENGTH_SHORT);
                    }
                })
                .setNegativeButton("Cancel", (d, w) -> adminWarningActions.setVisibility(View.VISIBLE))
                .show();
    }
}