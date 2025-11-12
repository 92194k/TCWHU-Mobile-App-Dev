package com.tcwhu.app;

import java.util.List;

public class Chat {
    private String lastMessage;
    private long timestamp;
    private List<String> users;

    // --- ADDED: Fields for Read/Unread status ---
    private String lastSenderId; // The ID of the user who sent the last message
    private boolean read; // Has the OTHER user read this last message?

    // Required empty constructor for Firestore
    public Chat() {}

    public String getLastMessage() { return lastMessage; }
    public long getTimestamp() { return timestamp; }
    public List<String> getUsers() { return users; }

    // --- ADDED: Getters for new fields ---
    public String getLastSenderId() { return lastSenderId; }
    public boolean isRead() { return read; }
}