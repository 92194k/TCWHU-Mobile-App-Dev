package com.tcwhu.app;

public class Report {
    private String id; // Add this
    private String reporterId;
    private String reportedId;
    private String reason;
    private long timestamp;

    // Required empty constructor for Firestore
    public Report() {}

    public Report(String reporterId, String reportedId, String reason, long timestamp) {
        this.reporterId = reporterId;
        this.reportedId = reportedId;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }

    public String getReportedId() { return reportedId; }
    public void setReportedId(String reportedId) { this.reportedId = reportedId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
