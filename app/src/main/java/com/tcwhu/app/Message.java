package com.tcwhu.app;

import com.google.firebase.firestore.PropertyName;

public class Message {

    private String senderId;
    private String content;
    private String type;
    private long timestamp;
    private boolean seen;

    private String messageId;
    private String fileName;
    private int status;
    private long mediaDuration;

    public Message() {
        this.seen = false;
        this.status = 0;
        this.mediaDuration = 0;
    }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @PropertyName("seen")
    public boolean isSeen() { return seen; }

    @PropertyName("seen")
    public void setSeen(boolean seen) { this.seen = seen; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public long getMediaDuration() { return mediaDuration; }
    public void setMediaDuration(long mediaDuration) { this.mediaDuration = mediaDuration; }
}