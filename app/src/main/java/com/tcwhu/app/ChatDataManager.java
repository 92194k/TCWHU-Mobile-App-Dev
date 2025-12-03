package com.tcwhu.app;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;

// NOTE: Placeholder classes (Message, Chat, ChatWindowCallbacks) are assumed to exist.

public class ChatDataManager {

    private final FirebaseFirestore db;
    private final String currentUserId;
    private final String otherUserId;
    private final String chatId;
    private final ChatWindowCallbacks callbacks;
    private final List<Message> messageList;

    private ListenerRegistration chatListener;
    private long userDeletionTimestamp = 0; // Soft-delete marker

    public ChatDataManager(String currentUserId, String otherUserId, String chatId,
                           List<Message> messageList, ChatWindowCallbacks callbacks) {
        this.db = FirebaseFirestore.getInstance();
        this.currentUserId = currentUserId;
        this.otherUserId = otherUserId;
        this.chatId = chatId;
        this.messageList = messageList;
        this.callbacks = callbacks;
    }

    // --- User Details ---

    public void loadChatPartnerDetails() {
        db.collection("users").document(otherUserId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String nickname = doc.getString("nickname");
                        String year = doc.getString("yearLevel");
                        String role = doc.getString("role");
                        callbacks.onChatPartnerDetailsLoaded(nickname, year, role);
                    }
                });
    }

    // --- Message Listener and Filtering (Soft Delete Logic) ---

    public void listenForMessages() {
        DocumentReference chatRef = db.collection("chats").document(chatId);

        chatRef.get().addOnSuccessListener(doc -> {
            userDeletionTimestamp = 0;

            if (doc.exists()) {
                Map<String, Object> deletedAtMap = (Map<String, Object>) doc.get("deletedAt");

                if (deletedAtMap != null && deletedAtMap.containsKey(currentUserId)) {
                    Long timestamp = (Long) deletedAtMap.get(currentUserId);
                    if (timestamp != null) {
                        userDeletionTimestamp = timestamp;
                    }
                }
            }

            messageList.clear();
            setupMessageListener();
        }).addOnFailureListener(e -> {
            Log.e("ChatDataManager", "Failed to check chat soft-delete status.", e);
            messageList.clear();
            setupMessageListener();
        });
    }

    private void setupMessageListener() {
        if (chatListener != null) {
            chatListener.remove();
        }

        CollectionReference messagesRef = db.collection("chats")
                .document(chatId).collection("messages");

        Query query = messagesRef.orderBy("timestamp", Query.Direction.ASCENDING);

        if (userDeletionTimestamp > 0) {
            query = query.whereGreaterThan("timestamp", userDeletionTimestamp);
        }

        chatListener = query
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        Log.e("ChatDataManager", "Error listening for messages: " + error.getMessage());
                        return;
                    }

                    boolean atBottom = true;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        Message m = dc.getDocument().toObject(Message.class);
                        m.setMessageId(dc.getDocument().getId());


                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            messageList.add(m);

                            if (!m.isSeen() && !m.getSenderId().equals(currentUserId)) {
                                markMessageAsSeen(dc.getDocument().getReference());
                            }

                        } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
                            for (int i = 0; i < messageList.size(); i++) {
                                if (Objects.equals(messageList.get(i).getMessageId(), m.getMessageId())) {
                                    messageList.set(i, m);
                                    break;
                                }
                            }
                        } else if (dc.getType() == DocumentChange.Type.REMOVED) {
                            for (int i = 0; i < messageList.size(); i++) {
                                if (Objects.equals(messageList.get(i).getMessageId(), m.getMessageId())) {
                                    messageList.remove(i);
                                    break;
                                }
                            }
                        }
                    }
                    callbacks.onMessageListUpdated(messageList, atBottom);
                });
    }

    public void markMessageAsSeen(DocumentReference ref) {
        ref.update("seen", true);
    }

    public void markChatAsRead() {
        db.collection("chats").document(chatId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Logic to mark chat as read
                    }
                });
    }

    public void cleanupListener() {
        if (chatListener != null) chatListener.remove();
    }

    // --- Message Sending ---

    public void sendMessage(String type, String content, String fileName, long durationMillis) {
        if (type.equals("text") && content.trim().isEmpty()) return;

        String normalizedType = type.toLowerCase(Locale.getDefault());
        String lastMessage;

        // --- Generate lastMessage summary for chat overview ---
        switch (normalizedType) {
            case "text":
                lastMessage = content;
                break;
            case "image":
                lastMessage = "[Image]";
                break;
            case "file":
                lastMessage = "[File: " + (fileName != null ? fileName : "Document") + "]";
                break;
            case "audio":
                // Use the duration to create a descriptive last message summary
                String durationDisplay = formatDurationForSummary(durationMillis);
                lastMessage = "[Voice Message (" + durationDisplay + ")]";
                break;
            case "video":
                lastMessage = "[Video]";
                break;
            default:
                lastMessage = "[Media]";
                break;
        }

        saveMessageToFirestore(type, content, fileName, durationMillis, () -> {
            updateChatOverview(lastMessage, false);
            callbacks.showToast("Message sent.", Toast.LENGTH_SHORT);
            resetDeletionMarker();
        });
    }

    private String formatDurationForSummary(long durationMillis) {
        if (durationMillis <= 0) return "0:00"; // FIX: Default to 0:00 instead of "--:--"
        int totalSeconds = (int) (durationMillis / 1000);
        int seconds = totalSeconds % 60;
        int minutes = totalSeconds / 60;

        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    private void resetDeletionMarker() {
        if (userDeletionTimestamp == 0) {
            return;
        }

        DocumentReference chatRef = db.collection("chats").document(chatId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("deletedAt." + currentUserId, FieldValue.delete());

        chatRef.update(updates)
                .addOnSuccessListener(v -> {
                    userDeletionTimestamp = 0;
                    Log.d("ChatDataManager", "Soft-delete marker cleared.");
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatDataManager", "Failed to reset soft-delete marker.", e);
                });
    }

    public void saveMessageToFirestore(String type, String content, String fileName, long durationMillis, Runnable success) {
        Map<String, Object> msg = new HashMap<>();

        // --- CRITICAL NULL SAFETY CHECK ---
        if (currentUserId == null) {
            callbacks.showToast("Sender ID is null. Cannot send.", Toast.LENGTH_SHORT);
            return;
        }

        msg.put("senderId", currentUserId);
        msg.put("content", content);
        msg.put("type", type);
        msg.put("timestamp", System.currentTimeMillis());
        msg.put("seen", false);
        msg.put("status", 0);

        String normalizedType = type.toLowerCase(Locale.getDefault());

        if (("file".equals(normalizedType) || "audio".equals(normalizedType) || "video".equals(normalizedType)) && fileName != null) {
            msg.put("fileName", fileName);
        }

        if (durationMillis > 0) {
            msg.put("mediaDuration", durationMillis);
        }

        db.collection("chats").document(chatId)
                .collection("messages")
                .add(msg)
                .addOnSuccessListener(ref -> {
                    if (success != null) success.run();
                })
                .addOnFailureListener(e ->
                        callbacks.showToast("Failed to send.", Toast.LENGTH_SHORT));
    }

    public void updateChatOverview(String lastMessage, boolean isAcknowledgment) {

        // --- CRITICAL NULL SAFETY CHECK ---
        if (currentUserId == null || otherUserId == null) {
            Log.e("ChatDataManager", "Cannot update overview: User IDs are null.");
            return;
        }

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

    public void setupAdminWarningUI(boolean isChatWithAdmin, boolean amIStudent) {
        if (isChatWithAdmin && amIStudent) {
            callbacks.setInputContainerVisibility(View.GONE);

            db.collection("chats").document(chatId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() &&
                                doc.getBoolean("warningAcknowledged") != null &&
                                doc.getBoolean("warningAcknowledged")) {
                            callbacks.setAdminWarningActionsVisibility(View.GONE);
                        } else {
                            callbacks.setAdminWarningActionsVisibility(View.VISIBLE);
                        }
                    });

        } else {
            callbacks.setInputContainerVisibility(View.VISIBLE);
            callbacks.setAdminWarningActionsVisibility(View.GONE);
        }
    }

    public void sendConfirmationMessage(String acknowledgedMessage) {
        // FIX: Reverting to the reliable saveMessageToFirestore callback chain.
        saveMessageToFirestore("text", acknowledgedMessage, null, 0, () -> {
            updateChatOverview(acknowledgedMessage, true);
            callbacks.showToast("Warning acknowledged.", Toast.LENGTH_SHORT);
        });
    }


    // --- Deletion Logic ---

    public void deleteConversation() {
        DocumentReference chatRef = db.collection("chats").document(chatId);

        chatRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String otherUser = otherUserId;
                Map<String, Object> deletedAtMap = (Map<String, Object>) doc.get("deletedAt");

                boolean otherUserAlreadyDeleted = deletedAtMap != null && otherUser != null
                        && deletedAtMap.containsKey(otherUser);

                if (otherUserAlreadyDeleted) {
                    performHardDelete(chatRef.collection("messages"));
                } else {
                    performSoftDelete(chatRef);
                }

            } else {
                callbacks.showToast("Conversation not found.", Toast.LENGTH_SHORT);
                callbacks.finishActivity();
            }
        }).addOnFailureListener(e -> {
            callbacks.showToast("Failed to check chat status.", Toast.LENGTH_SHORT);
        });
    }

    private void performSoftDelete(DocumentReference chatRef) {
        long deletionTime = System.currentTimeMillis();

        Map<String, Object> update = new HashMap<>();
        update.put("deletedAt." + currentUserId, deletionTime);

        chatRef.update(update)
                .addOnSuccessListener(v -> {
                    callbacks.showToast("Conversation history cleared.", Toast.LENGTH_SHORT);
                    userDeletionTimestamp = deletionTime;
                    messageList.clear();
                    callbacks.finishActivity();
                })
                .addOnFailureListener(e ->
                        callbacks.showToast("Failed to clear history.", Toast.LENGTH_SHORT));
    }

    private void performHardDelete(CollectionReference messagesRef) {
        messagesRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                WriteBatch batch = db.batch();

                for (QueryDocumentSnapshot doc : task.getResult()) {
                    batch.delete(doc.getReference());
                }

                batch.delete(db.collection("chats").document(chatId));

                batch.commit()
                        .addOnSuccessListener(v -> {
                            callbacks.showToast("Conversation permanently deleted.", Toast.LENGTH_SHORT);
                            callbacks.finishActivity();
                        })
                        .addOnFailureListener(e ->
                                callbacks.showToast("Failed to delete permanently.", Toast.LENGTH_SHORT));
            } else {
                callbacks.showToast("Failed to fetch messages for deletion.", Toast.LENGTH_SHORT);
            }
        });
    }
}