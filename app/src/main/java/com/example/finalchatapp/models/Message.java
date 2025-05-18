package com.example.finalchatapp.models;

public class Message {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String content;
    private String imageUrl; // New field for image URL
    private int type; // Message type: 0=text, 1=image
    private long timestamp;
    private boolean seen;

    // Constants for message types
    public static final int TYPE_TEXT = 0;
    public static final int TYPE_IMAGE = 1;

    // Empty constructor for Firestore
    public Message() {
        this.type = TYPE_TEXT; // Default to text message
    }

    // Constructor for text messages (your existing constructor)
    public Message(String messageId, String senderId, String receiverId, String content) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.seen = false;
        this.type = TYPE_TEXT;
    }

    // New constructor for image messages
    public Message(String messageId, String senderId, String receiverId, String imageUrl, int type) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.imageUrl = imageUrl;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.seen = false;
    }

    // Your existing getters and setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    // New getters and setters for image support
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}