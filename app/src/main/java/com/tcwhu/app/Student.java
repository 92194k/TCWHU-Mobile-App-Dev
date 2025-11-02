package com.tcwhu.app;

import com.google.firebase.firestore.Exclude;
import java.io.Serializable;
import java.util.List;

public class Student implements Serializable {
    @Exclude private String userId;
    private String studentNumber;
    private String nickname;
    private String yearLevel;
    private String interests;
    private String avatar;
    private String email;
    private boolean isVerified;
    private boolean isBanned;
    private boolean isSuspended;
    private long createdAt;
    private String selfiePhotoUrl;
    private String idPhotoUrl;
    private List<String> blockedUsers; // <-- ADDED

    public Student() {}

    // --- Getters ---
    public String getUserId() { return userId; }
    public String getStudentNumber() { return studentNumber; }
    public String getNickname() { return nickname; }
    public String getYearLevel() { return yearLevel; }
    public String getInterests() { return interests; }
    public String getAvatar() { return avatar; }
    public String getEmail() { return email; }
    public boolean isVerified() { return isVerified; }
    public boolean isBanned() { return isBanned; }
    public boolean isSuspended() { return isSuspended; }
    public long getCreatedAt() { return createdAt; }
    public String getSelfiePhotoUrl() { return selfiePhotoUrl; }
    public String getIdPhotoUrl() { return idPhotoUrl; }
    public List<String> getBlockedUsers() { return blockedUsers; } // <-- ADDED

    // --- Setter ---
    public void setUserId(String userId) { this.userId = userId; }
    // (Other setters are handled by Firestore)
}