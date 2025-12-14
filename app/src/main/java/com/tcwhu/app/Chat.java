package com.tcwhu.app;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import java.util.List;
import java.util.Map;

public class Chat {

    @Exclude private String chatId;
    private String lastMessage;
    private long timestamp;
    private List<String> users;
    private String lastSenderId;
    private boolean read;
    private boolean warningAcknowledged;
    private Map<String, Long> deletedAt;

    public Chat() {}

    @Exclude
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public List<String> getUsers() { return users; }
    public void setUsers(List<String> users) { this.users = users; }

    public String getLastSenderId() { return lastSenderId; }
    public void setLastSenderId(String lastSenderId) { this.lastSenderId = lastSenderId; }

    @PropertyName("read")
    public boolean isRead() { return read; }
    @PropertyName("read")
    public void setRead(boolean read) { this.read = read; }

    @PropertyName("warningAcknowledged")
    public boolean isWarningAcknowledged() { return warningAcknowledged; }
    @PropertyName("warningAcknowledged")
    public void setWarningAcknowledged(boolean warningAcknowledged) { this.warningAcknowledged = warningAcknowledged; }

    public Map<String, Long> getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Map<String, Long> deletedAt) { this.deletedAt = deletedAt; }
}