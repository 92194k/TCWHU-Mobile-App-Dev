package com.tcwhu.app;

import com.google.firebase.firestore.Exclude;

public class AdminAccount {
    @Exclude private String id;
    private String email;
    private String role;
    private long addedDate;

    public AdminAccount() {}

    // Getters
    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public long getAddedDate() { return addedDate; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role) { this.role = role; }
    public void setAddedDate(long addedDate) { this.addedDate = addedDate; }
}