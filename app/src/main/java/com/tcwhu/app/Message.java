package com.tcwhu.app;

// NOTE: Add imports as necessary for other existing Message fields if required, but none are visible here.

public class Message {

    // Core Fields (Existing)
    private String senderId;
    private String content; // Stores text or media URL
    private String type;
    private long timestamp;
    private boolean seen;

    // --- NEW FIELDS ADDED FOR FEATURES ---
    private String messageId; // Unique ID from Firestore Document (Required for Deletion/Updates)
    private String fileName;  // Display name for documents (e.g., "report.pdf")
    private int status;       // 0: Active, 1: Deleted For Everyone, 2: Uploading (NEW)
    // -------------------------------------

    public Message() {
        this.seen = false;      // Default to not seen
        this.status = 0;        // Default to Active (or 2 for initial placeholder if sent immediately)
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


    // --- Existing Setters ---
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setContent(String content) { this.content = content; }
    public void setType(String type) { this.type = type; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setSeen(boolean seen) { this.seen = seen; }


    // --- NEW/MODIFIED Setters (Crucial for Listener and Upload Status) ---
    // The messageId must be set manually after fetching from Firestore snapshot.
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}