package com.tcwhu.app;

import com.google.firebase.firestore.Exclude;

public class Report {
    @Exclude private String id;
    private String reporterId;
    private String reportedUserId;
    private String reason;
    private long timestamp;
    private String status;
    private boolean resolved;

    public Report() {}

    public Report(String reporterId, String reportedUserId, String reason) {
        this.reporterId = reporterId;
        this.reportedUserId = reportedUserId;
        this.reason = reason;
        this.timestamp = System.currentTimeMillis();
        this.status = "pending";
        this.resolved = false;
    }

    @Exclude public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }

    public String getReportedUserId() { return reportedUserId; }
    public void setReportedUserId(String reportedUserId) { this.reportedUserId = reportedUserId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
}