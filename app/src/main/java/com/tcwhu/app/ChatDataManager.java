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
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Objects;


// NOTE: Placeholder classes (Message, Chat, ChatWindowCallbacks) are assumed to exist.
// The Message class must include setMessageId(String) and getMessageId() methods.

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

    /**
     * Fetches the soft-delete timestamp from Firestore FIRST, then calls setupMessageListener()
     * only after the value is successfully loaded or confirmed absent.
     */
    public void listenForMessages() {
        DocumentReference chatRef = db.collection("chats").document(chatId);

        // 1. Check if this user (currentUserId) has a soft-delete timestamp.
        chatRef.get().addOnSuccessListener(doc -> {
            userDeletionTimestamp = 0; // Reset local marker before checking Firestore

            if (doc.exists()) {
                // Safely retrieve the nested map and the specific user's timestamp
                Map<String, Object> deletedAtMap = (Map<String, Object>) doc.get("deletedAt");

                if (deletedAtMap != null && deletedAtMap.containsKey(currentUserId)) {
                    // Firestore stores numbers as Long by default.
                    Long timestamp = (Long) deletedAtMap.get(currentUserId);
                    if (timestamp != null) {
                        userDeletionTimestamp = timestamp;
                    }
                }
            }

            // 2. Clear the list and setup the listener with the correct filter.
            messageList.clear();
            setupMessageListener();
        }).addOnFailureListener(e -> {
            Log.e("ChatDataManager", "Failed to check chat soft-delete status.", e);
            messageList.clear();
            setupMessageListener();
        });
    }

    /**
     * Sets up the Firestore listener, applying the soft-delete timestamp as a filter if present.
     */
    private void setupMessageListener() {
        // Remove existing listener before setting a new one
        if (chatListener != null) {
            chatListener.remove();
        }

        CollectionReference messagesRef = db.collection("chats")
                .document(chatId).collection("messages");

        Query query = messagesRef.orderBy("timestamp", Query.Direction.ASCENDING);

        // 3. APPLY SOFT-DELETE FILTER: If userDeletionTimestamp > 0, only new messages are fetched.
        if (userDeletionTimestamp > 0) {
            // Filter applied based on the timestamp fetched in listenForMessages()
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
                        // Enhance: Set the Firestore document ID for reliable updates (single message delete fix)
                        m.setMessageId(dc.getDocument().getId());


                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            messageList.add(m);

                            if (!m.isSeen() && !m.getSenderId().equals(currentUserId)) {
                                markMessageAsSeen(dc.getDocument().getReference());
                            }

                        } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
                            // Use MessageId for reliable update (needed for single message delete status change)
                            for (int i = 0; i < messageList.size(); i++) {
                                if (Objects.equals(messageList.get(i).getMessageId(), m.getMessageId())) {
                                    messageList.set(i, m);
                                    break;
                                }
                            }
                        } else if (dc.getType() == DocumentChange.Type.REMOVED) {
                            // Use MessageId for reliable removal
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
                        // Assuming Chat class exists with getLastSenderId() and getUsers()
                        // Chat chat = doc.toObject(Chat.class);
                        // if (chat != null && chat.getLastSenderId() != null && !chat.getLastSenderId().equals(currentUserId)) {
                        //     doc.getReference().update("read", true);
                        // }
                    }
                });
    }

    public void cleanupListener() {
        if (chatListener != null) chatListener.remove();
    }

    // --- Message Sending (Stable Soft-Delete Reset) ---

    public void sendMessage(String text, String type) {
        sendMessage("text", text, null);
    }

    public void sendMessage(String type, String content, String fileName) {
        if (type.equals("text") && content.trim().isEmpty()) return;

        String normalizedType = type.toLowerCase();
        String lastMessage;

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
                lastMessage = "[Audio]";
                break;
            case "video":
                lastMessage = "[Video]";
                break;
            default:
                lastMessage = "[Media]";
                break;
        }

        // Save the message, then update the overview in the success callback.
        saveMessageToFirestore(type, content, fileName, () -> {
            updateChatOverview(lastMessage, false);

            callbacks.showToast("Message sent.", Toast.LENGTH_SHORT);

            // FIX: Remove the deletion filter without restarting the query.
            resetDeletionMarker();
        });
    }

    /**
     * FIX INCORPORATED: This method removes the soft-delete marker from Firestore and locally
     * without clearing the message list or restarting the listener, thus preventing the
     * reappearance of old messages. The existing live listener continues to receive new data.
     */
    private void resetDeletionMarker() {
        if (userDeletionTimestamp == 0) {
            // Already reset, no action needed
            return;
        }

        DocumentReference chatRef = db.collection("chats").document(chatId);

        // Use FieldValue.delete() to remove the specific user's entry from the deletedAt map
        Map<String, Object> updates = new HashMap<>();
        updates.put("deletedAt." + currentUserId, FieldValue.delete());

        chatRef.update(updates)
                .addOnSuccessListener(v -> {
                    // 1. Reset the local filter marker to 0. This ensures that
                    // if the user navigates away and comes back, no filter is applied.
                    userDeletionTimestamp = 0;

                    // 2. IMPORTANT: We rely on the existing listener to now automatically
                    // switch to an unfiltered view since the document field is gone.

                    Log.d("ChatDataManager", "Soft-delete marker cleared from Firestore. New convo segment is live.");
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatDataManager", "Failed to reset soft-delete marker.", e);
                });
    }

    public void saveMessageToFirestore(String type, String content, String fileName, Runnable success) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", currentUserId);
        msg.put("content", content);
        msg.put("type", type);
        msg.put("timestamp", System.currentTimeMillis());
        msg.put("seen", false);
        msg.put("status", 0);

        String normalizedType = type.toLowerCase();
        if (("file".equals(normalizedType) || "audio".equals(normalizedType) || "video".equals(normalizedType)) && fileName != null) {
            msg.put("fileName", fileName);
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
        saveMessageToFirestore("text", acknowledgedMessage, null, () -> {
            updateChatOverview(acknowledgedMessage, true);
            callbacks.showToast("Warning acknowledged.", Toast.LENGTH_SHORT);
        });
    }


    // --- Deletion Logic ---

    public void deleteConversation() {
        DocumentReference chatRef = db.collection("chats").document(chatId);

        chatRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                // Placeholder logic for checking other user's deletion status
                String otherUser = otherUserId; // Simplified assumption
                Map<String, Object> deletedAtMap = (Map<String, Object>) doc.get("deletedAt");

                boolean otherUserAlreadyDeleted = deletedAtMap != null && otherUser != null
                        && deletedAtMap.containsKey(otherUser);

                if (otherUserAlreadyDeleted) {
                    // Both users deleted -> Hard Delete
                    performHardDelete(chatRef.collection("messages"));
                } else {
                    // Only this user deleted -> Soft Delete
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

    /**
     * Records the time of deletion for this specific user.
     */
    private void performSoftDelete(DocumentReference chatRef) {
        long deletionTime = System.currentTimeMillis();

        Map<String, Object> update = new HashMap<>();
        update.put("deletedAt." + currentUserId, deletionTime);

        chatRef.update(update)
                .addOnSuccessListener(v -> {
                    callbacks.showToast("Conversation history cleared.", Toast.LENGTH_SHORT);

                    // Update local marker to reflect the deletion immediately
                    userDeletionTimestamp = deletionTime;

                    messageList.clear(); // Clear local list to hide history immediately
                    callbacks.finishActivity(); // Navigates away
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