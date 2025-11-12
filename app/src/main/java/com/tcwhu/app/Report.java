package com.tcwhu.app;

import com.google.firebase.firestore.Exclude;

public class Report {
    @Exclude private String id;
    private String reporterId;
    private String reportedUserId;
    private String reason;
    private long timestamp;
    private String status; // "pending", "resolved"

    public Report() {} // Required for Firestore

    public Report(String reporterId, String reportedUserId, String reason) {
        this.reporterId = reporterId;
        this.reportedUserId = reportedUserId;
        this.reason = reason;
        this.timestamp = System.currentTimeMillis();
        this.status = "pending";
    }

    // Getters
    public String getId() { return id; }
    public String getReporterId() { return reporterId; }
    public String getReportedUserId() { return reportedUserId; }
    public String getReason() { return reason; }
    public long getTimestamp() { return timestamp; }
    public String getStatus() { return status; }

    // Setter
    public void setId(String id) { this.id = id; }
}