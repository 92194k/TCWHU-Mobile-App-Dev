package com.tcwhu.app;

import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChatDataManager {

    private final FirebaseFirestore db;
    private final String currentUserId;
    private final String otherUserId;
    private final String chatId;
    private final List<Message> messageList;
    private final ChatWindowCallbacks callbacks;
    private ListenerRegistration chatListener;
    private long userClearedTimestamp = 0;

    public ChatDataManager(String currentUserId, String otherUserId, String chatId,
                           List<Message> messageList, ChatWindowCallbacks callbacks) {
        this.db = FirebaseFirestore.getInstance();
        this.currentUserId = currentUserId;
        this.otherUserId = otherUserId;
        this.chatId = chatId;
        this.messageList = messageList;
        this.callbacks = callbacks;
    }

    public void loadChatPartnerDetails() {
        db.collection("users").document(otherUserId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        callbacks.onChatPartnerDetailsLoaded(
                                doc.getString("nickname"),
                                doc.getString("yearLevel"),
                                doc.getString("role")
                        );
                    }
                });
    }

    @SuppressWarnings("unchecked")
    public void listenForMessages() {
        DocumentReference chatRef = db.collection("chats").document(chatId);

        chatRef.get().addOnSuccessListener(doc -> {
            userClearedTimestamp = 0;
            if (doc.exists()) {
                // Check if user has cleared history
                Map<String, Object> clearedMap = (Map<String, Object>) doc.get("clearedAt");
                Map<String, Object> deletedMap = (Map<String, Object>) doc.get("deletedAt");

                if (clearedMap != null && clearedMap.containsKey(currentUserId)) {
                    Long ts = (Long) clearedMap.get(currentUserId);
                    if (ts != null) userClearedTimestamp = ts;
                } else if (deletedMap != null && deletedMap.containsKey(currentUserId)) {
                    Long ts = (Long) deletedMap.get(currentUserId);
                    if (ts != null) userClearedTimestamp = ts;
                }
            }
            messageList.clear();
            setupMessageListener();
        }).addOnFailureListener(e -> {
            messageList.clear();
            setupMessageListener();
        });
    }

    private void setupMessageListener() {
        if (chatListener != null) chatListener.remove();

        CollectionReference messagesRef = db.collection("chats").document(chatId).collection("messages");
        Query query = messagesRef.orderBy("timestamp", Query.Direction.ASCENDING);

        if (userClearedTimestamp > 0) {
            query = query.whereGreaterThan("timestamp", userClearedTimestamp);
        }

        chatListener = query.addSnapshotListener((snapshots, error) -> {
            if (error != null || snapshots == null) return;

            boolean atBottom = true;

            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                Message m = dc.getDocument().toObject(Message.class);
                m.setMessageId(dc.getDocument().getId());

                switch (dc.getType()) {
                    case ADDED:
                        messageList.add(m);
                        if (!m.isSeen() && !m.getSenderId().equals(currentUserId)) {
                            markMessageAsSeen(dc.getDocument().getReference());
                        }
                        break;
                    case MODIFIED:
                        updateMessageInList(m);
                        break;
                    case REMOVED:
                        removeMessageFromList(m);
                        break;
                }
            }
            callbacks.onMessageListUpdated(messageList, atBottom);
        });
    }

    private void updateMessageInList(Message m) {
        for (int i = 0; i < messageList.size(); i++) {
            if (Objects.equals(messageList.get(i).getMessageId(), m.getMessageId())) {
                messageList.set(i, m);
                break;
            }
        }
    }

    private void removeMessageFromList(Message m) {
        for (int i = 0; i < messageList.size(); i++) {
            if (Objects.equals(messageList.get(i).getMessageId(), m.getMessageId())) {
                messageList.remove(i);
                break;
            }
        }
    }

    public void markMessageAsSeen(DocumentReference ref) {
        ref.update("seen", true);
    }

    public void markChatAsRead() {
        db.collection("chats").document(chatId).update("read", true);
    }

    public void cleanupListener() {
        if (chatListener != null) chatListener.remove();
    }

    // --- Message Sending ---

    public void sendMessage(String type, String content, String fileName, long durationMillis) {
        if (type.equals("text") && content.trim().isEmpty()) return;

        String lastMessage;
        switch (type.toLowerCase()) {
            case "image": lastMessage = "[Image]"; break;
            case "file": lastMessage = "[File]"; break;
            case "audio": lastMessage = "[Voice Message]"; break;
            case "video": lastMessage = "[Video]"; break;
            default: lastMessage = content; break;
        }

        saveMessageToFirestore(type, content, fileName, durationMillis, () -> {
            updateChatOverview(lastMessage, false);
            restoreChatVisibility();
        });
    }

    private void restoreChatVisibility() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("deletedAt." + currentUserId, FieldValue.delete());

        db.collection("chats").document(chatId).update(updates)
                .addOnFailureListener(e -> Log.e("ChatDataManager", "Failed to restore chat visibility"));
    }

    public void saveMessageToFirestore(String type, String content, String fileName, long durationMillis, Runnable success) {
        if (currentUserId == null) return;

        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", currentUserId);
        msg.put("content", content);
        msg.put("type", type);
        msg.put("timestamp", System.currentTimeMillis());
        msg.put("seen", false);
        msg.put("status", 0);

        if (fileName != null) msg.put("fileName", fileName);
        if (durationMillis > 0) msg.put("mediaDuration", durationMillis);

        db.collection("chats").document(chatId).collection("messages").add(msg)
                .addOnSuccessListener(ref -> { if (success != null) success.run(); })
                .addOnFailureListener(e -> callbacks.showToast("Failed to send.", Toast.LENGTH_SHORT));
    }

    public void updateChatOverview(String lastMessage, boolean isAcknowledgment) {
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("lastMessage", lastMessage);
        chatData.put("timestamp", System.currentTimeMillis());
        chatData.put("users", Arrays.asList(currentUserId, otherUserId));
        chatData.put("lastSenderId", currentUserId);
        chatData.put("read", false);

        if (isAcknowledgment) chatData.put("warningAcknowledged", true);

        db.collection("chats").document(chatId).update(chatData)
                .addOnFailureListener(e -> db.collection("chats").document(chatId).set(chatData));
    }

    public void setupAdminWarningUI(boolean isChatWithAdmin, boolean amIStudent) {
        if (isChatWithAdmin && amIStudent) {
            callbacks.setInputContainerVisibility(View.GONE);
            db.collection("chats").document(chatId).get().addOnSuccessListener(doc -> {
                if (doc.exists() && Boolean.TRUE.equals(doc.getBoolean("warningAcknowledged"))) {
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

    public void sendConfirmationMessage(String msg) {
        saveMessageToFirestore("text", msg, null, 0, () -> {
            updateChatOverview(msg, true);
            restoreChatVisibility();
            callbacks.showToast("Warning acknowledged.", Toast.LENGTH_SHORT);
        });
    }

    public void deleteConversation() {
        long deletionTime = System.currentTimeMillis();
        Map<String, Object> update = new HashMap<>();

        // 'deletedAt' hides it from the list, 'clearedAt' hides messages in the window
        update.put("deletedAt." + currentUserId, deletionTime);
        update.put("clearedAt." + currentUserId, deletionTime);

        db.collection("chats").document(chatId).update(update)
                .addOnSuccessListener(v -> {
                    userClearedTimestamp = deletionTime;
                    messageList.clear();
                    callbacks.onMessageListUpdated(messageList, true);
                    callbacks.finishActivity();
                });
    }
}