package com.tcwhu.app;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
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
    private long createdAt;
    private String selfiePhotoUrl;
    private String idPhotoUrl;
    private List<String> blockedUsers;
    private long suspendEndDate;
    private long deletionDate;
    private String deletionReason;

    private Boolean isVerified;
    private Boolean isBanned;
    private Boolean isSuspended;
    private Boolean isDeletionRequested;
    private Integer warningCount;

    private String role; // <-- ADDED

    public Student() {}

    // --- Getters ---
    @Exclude
    public String getUserId() { return userId; }
    public String getStudentNumber() { return studentNumber; }
    public String getNickname() { return nickname; }
    public String getYearLevel() { return yearLevel; }
    public String getInterests() { return interests; }
    public String getAvatar() { return avatar; }
    public String getEmail() { return email; }
    public long getCreatedAt() { return createdAt; }
    public String getSelfiePhotoUrl() { return selfiePhotoUrl; }
    public String getIdPhotoUrl() { return idPhotoUrl; }
    public List<String> getBlockedUsers() { return blockedUsers; }
    public long getSuspendEndDate() { return suspendEndDate; }
    public long getDeletionDate() { return deletionDate; }
    public String getDeletionReason() { return deletionReason; }
    public Integer getWarningCount() { return warningCount != null ? warningCount : 0; }
    public String getRole() { return role; } // <-- ADDED

    @PropertyName("isVerified")
    public boolean isVerified() {
        return isVerified != null && isVerified;
    }
    @PropertyName("isBanned")
    public boolean isBanned() {
        return isBanned != null && isBanned;
    }
    @PropertyName("isSuspended")
    public boolean isSuspended() {
        return isSuspended != null && isSuspended;
    }
    @PropertyName("isDeletionRequested")
    public boolean isDeletionRequested() {
        return isDeletionRequested != null && isDeletionRequested;
    }

    // --- Setters ---
    public void setUserId(String userId) { this.userId = userId; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setYearLevel(String yearLevel) { this.yearLevel = yearLevel; }
    public void setInterests(String interests) { this.interests = interests; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public void setEmail(String email) { this.email = email; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setSelfiePhotoUrl(String selfiePhotoUrl) { this.selfiePhotoUrl = selfiePhotoUrl; }
    public void setIdPhotoUrl(String idPhotoUrl) { this.idPhotoUrl = idPhotoUrl; }
    public void setBlockedUsers(List<String> blockedUsers) { this.blockedUsers = blockedUsers; }
    public void setSuspendEndDate(long suspendEndDate) { this.suspendEndDate = suspendEndDate; }
    public void setDeletionDate(long deletionDate) { this.deletionDate = deletionDate; }
    public void setDeletionReason(String deletionReason) { this.deletionReason = deletionReason; }
    public void setWarningCount(Integer warningCount) { this.warningCount = warningCount; }
    public void setRole(String role) { this.role = role; } // <-- ADDM

    @PropertyName("isVerified")
    public void setVerified(boolean verified) { isVerified = verified; }
    @PropertyName("isBanned")
    public void setBanned(boolean banned) { isBanned = banned; }
    @PropertyName("isSuspended")
    public void setSuspended(boolean suspended) { isSuspended = suspended; }
    @PropertyName("isDeletionRequested")
    public void setDeletionRequested(boolean deletionRequested) { isDeletionRequested = deletionRequested; }
}