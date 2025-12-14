package com.tcwhu.app;

import com.google.firebase.firestore.Exclude;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Event implements Serializable {

    @Exclude
    private String id;

    private String title;
    private String description;
    private long date;
    private String imageUrl;
    private String postedBy;
    private String targetId;
    private String icon;

    public Event() {}

    public Event(String title, String description, long date, String imageUrl, String postedBy) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.imageUrl = imageUrl;
        this.postedBy = postedBy;
    }

    public Event(String title, String description, long date, String imageUrl, String postedBy, String targetId, String icon) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.imageUrl = imageUrl;
        this.postedBy = postedBy;
        this.targetId = targetId;
        this.icon = icon;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getPostedBy() { return postedBy; }
    public void setPostedBy(String postedBy) { this.postedBy = postedBy; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        return sdf.format(new Date(date));
    }
}