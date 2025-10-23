package com.tcwhu.app;

import com.google.firebase.firestore.Exclude;

public class Report {
    @Exclude private String id; // To hold the Firestore document ID

    private String reporterId;
    private String reportedId;
    private String reason;
    private long timestamp;
    private String status; // e.g., "pending", "resolved"

    // Required empty constructor for Firestore
    public Report() {}

    // --- Getters ---
    public String getId() { return id; }
    public String getReporterId() { return reporterId; }
    public String getReportedId() { return reportedId; }
    public String getReason() { return reason; }
    public long getTimestamp() { return timestamp; }
    public String getStatus() { return status; }

    // --- Setter for the ID ---
    public void setId(String id) { this.id = id; }
}