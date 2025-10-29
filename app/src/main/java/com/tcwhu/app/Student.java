package com.tcwhu.app;

import com.google.firebase.firestore.Exclude;

public class Student {
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

    // --- Setter ---
    public void setUserId(String userId) { this.userId = userId; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setYearLevel(String yearLevel) { this.yearLevel = yearLevel; }
    public void setInterests(String interests) { this.interests = interests; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public void setEmail(String email) { this.email = email; }
    public void setVerified(boolean verified) { isVerified = verified; }
    public void setBanned(boolean banned) { isBanned = banned; }
    public void setSuspended(boolean suspended) { isSuspended = suspended; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setSelfiePhotoUrl(String selfiePhotoUrl) { this.selfiePhotoUrl = selfiePhotoUrl; }
    public void setIdPhotoUrl(String idPhotoUrl) { this.idPhotoUrl = idPhotoUrl; }
}