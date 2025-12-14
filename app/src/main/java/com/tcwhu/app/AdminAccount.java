package com.tcwhu.app;

import com.google.firebase.firestore.Exclude;

public class AdminAccount {
    @Exclude private String id;
    private String email;
    private String role;
    private long addedDate;

    public AdminAccount() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public long getAddedDate() { return addedDate; }
    public void setAddedDate(long addedDate) { this.addedDate = addedDate; }
}