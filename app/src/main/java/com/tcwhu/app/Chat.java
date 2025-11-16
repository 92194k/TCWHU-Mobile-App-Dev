package com.tcwhu.app;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import java.util.List;

public class Chat {

    // The Firestore document ID (must be excluded from standard mapping)
    @Exclude
    private String chatId;

    private String lastMessage;
    private long timestamp;
    private List<String> users;
    private String lastSenderId;
    private boolean read;

    // --- NEW FIELD: Tracks if the student has clicked "Confirm" or "Appeal" ---
    private boolean warningAcknowledged;

    // Required empty constructor for Firestore
    public Chat() {}

    // --- GETTERS ---

    @Exclude
    public String getChatId() { return chatId; }

    public String getLastMessage() { return lastMessage; }
    public long getTimestamp() { return timestamp; }
    public List<String> getUsers() { return users; }
    public String getLastSenderId() { return lastSenderId; }

    // Use @PropertyName to correctly map Firestore fields, even if named differently (e.g., 'isRead' or 'read')
    @PropertyName("read")
    public boolean isRead() { return read; }

    // --- NEW FIELD GETTER ---
    @PropertyName("warningAcknowledged")
    public boolean isWarningAcknowledged() { return warningAcknowledged; }

    // --- SETTERS (Crucial for AdminChatListActivity and general data manipulation) ---

    @Exclude
    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public void setLastSenderId(String lastSenderId) {
        this.lastSenderId = lastSenderId;
    }

    @PropertyName("read")
    public void setRead(boolean read) {
        this.read = read;
    }

    // --- NEW FIELD SETTER ---
    @PropertyName("warningAcknowledged")
    public void setWarningAcknowledged(boolean warningAcknowledged) {
        this.warningAcknowledged = warningAcknowledged;
    }
}