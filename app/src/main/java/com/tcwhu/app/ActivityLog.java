package com.tcwhu.app;

import com.google.firebase.firestore.Exclude;
import java.io.Serializable;

public class ActivityLog implements Serializable {
    @Exclude private String id;
    private String adminId;
    private String action;
    private String targetId;
    private long timestamp;

    public ActivityLog() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAdminId() { return adminId; }
    public String getAction() { return action; }
    public String getTargetId() { return targetId; }
    public long getTimestamp() { return timestamp; }
}