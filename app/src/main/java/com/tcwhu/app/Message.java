package com.tcwhu.app;

public class Message {
    private String senderId;
    private String content;
    private String type; // e.g., "text"
    private long timestamp;

    // Required empty constructor for Firestore
    public Message() {}

    public String getSenderId() { return senderId; }
    public String getContent() { return content; }
    public String getType() { return type; }
    public long getTimestamp() { return timestamp; }
}
