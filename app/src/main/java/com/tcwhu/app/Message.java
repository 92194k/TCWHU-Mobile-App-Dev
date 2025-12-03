package com.tcwhu.app;

// NOTE: Add imports as necessary for other existing Message fields if required,
// but none are visible here.

public class Message {

    // Core Fields (Existing)
    private String senderId;
    private String content; // Stores text or media URL
    private String type;
    private long timestamp;
    private boolean seen;

    // --- NEW FIELDS ADDED FOR FEATURES ---
    private String messageId;
    private String fileName;
    private int status;
    private long mediaDuration; // <--- ADDED: Duration in milliseconds for media (e.g., audio/video)
    // -------------------------------------

    public Message() {
        this.seen = false;
        this.status = 0;
        this.mediaDuration = 0;
    }

    // --- Existing Getters ---
    public String getSenderId() { return senderId; }
    public String getContent() { return content; }
    public String getType() { return type; }
    public long getTimestamp() { return timestamp; }
    public boolean isSeen() { return seen; }

    // --- NEW Getters ---
    public String getMessageId() { return messageId; }
    public String getFileName() { return fileName; }
    public int getStatus() { return status; }
    public long getMediaDuration() { return mediaDuration; }


    // --- Existing Setters ---
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setContent(String content) { this.content = content; }
    public void setType(String type) { this.type = type; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // 🔥 FIXED: Changed 'this.setSeen(seen)' to 'this.seen = seen' to prevent StackOverflowError
    public void setSeen(boolean seen) { this.seen = seen; }


    // --- NEW/MODIFIED Setters ---
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setMediaDuration(long mediaDuration) {
        this.mediaDuration = mediaDuration;
    }
}