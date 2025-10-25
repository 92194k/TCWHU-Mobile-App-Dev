package com.tcwhu.app;

import com.google.firebase.firestore.Exclude;

public class ActivityLog {
    @Exclude private String id; // To hold the Firestore document ID

    private String adminId;
    private String action;
    private String targetId; // The user ID, event ID, or report ID acted upon
    private long timestamp;

    // Required empty constructor for Firestore
    public ActivityLog() {}

    // --- Getters ---
    public String getId() { return id; }
    public String getAdminId() { return adminId; }
    public String getAction() { return action; }
    public String getTargetId() { return targetId; }
    public long getTimestamp() { return timestamp; }

    // --- Setter for the ID ---
    public void setId(String id) { this.id = id; }
}