package com.tcwhu.app;

public class Message {
    private String senderId;
    private String content;
    private String type; // e.g., "text" or "image"
    private long timestamp;
    private boolean seen;

    // Required empty constructor for Firestore
    public Message() {
        this.seen = false; // Default to not seen
    }

    public String getSenderId() { return senderId; }
    public String getContent() { return content; }
    public String getType() { return type; }
    public long getTimestamp() { return timestamp; }
    public boolean isSeen() { return seen; }
}