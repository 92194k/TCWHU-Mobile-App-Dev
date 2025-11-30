package com.tcwhu.app;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import java.util.List;

public class Chat {

    @Exclude
    private String chatId;

    private String lastMessage;
    private long timestamp;
    private List<String> users;
    private String lastSenderId;
    private boolean read;
    private boolean warningAcknowledged;

    // --- NEW FIELD FOR SOFT DELETE ---
    private List<String> deletedBy; // List of user IDs who have soft-deleted the conversation

    public Chat() {}

    // Getters

    @Exclude
    public String getChatId() { return chatId; }

    public String getLastMessage() { return lastMessage; }
    public long getTimestamp() { return timestamp; }
    public List<String> getUsers() { return users; }
    public String getLastSenderId() { return lastSenderId; }

    @PropertyName("read")
    public boolean isRead() { return read; }

    @PropertyName("warningAcknowledged")
    public boolean isWarningAcknowledged() { return warningAcknowledged; }

    // --- NEW GETTER ---
    public List<String> getDeletedBy() { return deletedBy; }


    // SETTERS

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

    @PropertyName("warningAcknowledged")
    public void setWarningAcknowledged(boolean warningAcknowledged) {
        this.warningAcknowledged = warningAcknowledged;
    }

    // --- NEW SETTER ---
    public void setDeletedBy(List<String> deletedBy) {
        this.deletedBy = deletedBy;
    }
}