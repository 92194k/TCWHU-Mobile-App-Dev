package com.tcwhu.app;

import com.google.firebase.firestore.Exclude;
import java.io.Serializable; // <-- IMPORT ADDED

// Implement Serializable to allow passing this object between activities
public class ActivityLog implements Serializable {
    @Exclude private String id;
    private String adminId;
    private String action;
    private String targetId;
    private long timestamp;

    public ActivityLog() {} // Required for Firestore

    // --- Getters ---
    public String getId() { return id; }
    public String getAdminId() { return adminId; }
    public String getAction() { return action; }
    public String getTargetId() { return targetId; }
    public long getTimestamp() { return timestamp; }

    // --- Setters ---
    public void setId(String id) { this.id = id; }
}