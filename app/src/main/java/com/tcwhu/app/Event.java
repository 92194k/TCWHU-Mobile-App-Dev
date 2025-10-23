package com.tcwhu.app;

import com.google.firebase.firestore.Exclude;

public class Event {
    @Exclude private String id; // To store the Firestore document ID

    private String title;
    private String description;
    private long date;
    private String imageUrl;
    private String postedBy;

    // Required empty constructor for Firestore
    public Event() {}

    public Event(String title, String description, long date, String imageUrl, String postedBy) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.imageUrl = imageUrl;
        this.postedBy = postedBy;
    }

    // --- Getters and Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public long getDate() { return date; }
    public String getImageUrl() { return imageUrl; }
    public String getPostedBy() { return postedBy; }
}