package com.example.finalchatapp.models;

public class Chat {
    private String chatId;
    private String user1Id;
    private String user2Id;
    private String lastMessageContent;
    private long lastMessageTimestamp;
    private String lastMessageSenderId;

    // Empty constructor for Firestore
    public Chat() {}

    public Chat(String chatId, String user1Id, String user2Id) {
        this.chatId = chatId;
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.lastMessageContent = "";
        this.lastMessageTimestamp = System.currentTimeMillis();
        this.lastMessageSenderId = "";
    }

    // Getters and setters
    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getUser1Id() {
        return user1Id;
    }

    public void setUser1Id(String user1Id) {
        this.user1Id = user1Id;
    }

    public String getUser2Id() {
        return user2Id;
    }

    public void setUser2Id(String user2Id) {
        this.user2Id = user2Id;
    }

    public String getLastMessageContent() {
        return lastMessageContent;
    }

    public void setLastMessageContent(String lastMessageContent) {
        this.lastMessageContent = lastMessageContent;
    }

    public long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public String getLastMessageSenderId() {
        return lastMessageSenderId;
    }

    public void setLastMessageSenderId(String lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }
}