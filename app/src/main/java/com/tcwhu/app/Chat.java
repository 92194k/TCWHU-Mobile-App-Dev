package com.tcwhu.app;

import java.util.List;

public class Chat {
    private String lastMessage;
    private long timestamp;
    private List<String> users; // DEFINITIVE FIELD NAME: users

    // Required empty constructor for Firestore
    public Chat() {}

    public String getLastMessage() { return lastMessage; }
    public long getTimestamp() { return timestamp; }
    public List<String> getUsers() { return users; } // Correct getter
}