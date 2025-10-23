package com.tcwhu.app;

import com.google.firebase.firestore.Exclude;

public class Student {

    @Exclude private String userId; // We use this to hold the Firebase Auth ID

    // These fields will be saved to Firestore
    private String studentNumber;
    private String nickname;
    private String yearLevel;
    private String interests;
    private String avatar;
    private boolean isVerified;
    private long createdAt;

    // An empty constructor is required for Firestore to work
    public Student() {}

    // --- Getters and Setters ---
    // These allow our code to get and set the values for each field

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getStudentNumber() { return studentNumber; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getYearLevel() { return yearLevel; }
    public void setYearLevel(String yearLevel) { this.yearLevel = yearLevel; }

    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { this.isVerified = verified; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}