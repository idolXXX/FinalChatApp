package com.example.finalchatapp.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Message {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String content;
    private String imageUrl;
    private int type;
    private long timestamp;
    private boolean seen;
    private Map<String, List<String>> reactions;


    public static final int TYPE_TEXT = 0;
    public static final int TYPE_IMAGE = 1;


    public Message() {
        this.type = TYPE_TEXT;
        this.reactions = new HashMap<>();
    }


    public Message(String messageId, String senderId, String receiverId, String content) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.seen = false;
        this.type = TYPE_TEXT;
        this.reactions = new HashMap<>();
    }


    public Message(String messageId, String senderId, String receiverId, String imageUrl, int type) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.imageUrl = imageUrl;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.seen = false;
        this.reactions = new HashMap<>();
    }


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
    // Reactions methods
    public Map<String, List<String>> getReactions() {
        return reactions != null ? reactions : new HashMap<>();
    }

    public void setReactions(Map<String, List<String>> reactions) {
        this.reactions = reactions;
    }

    public void addReaction(String emoji, String userId) {
        if (reactions == null) {
            reactions = new HashMap<>();
        }

        if (!reactions.containsKey(emoji)) {
            reactions.put(emoji, new ArrayList<>());
        }

        if (!reactions.get(emoji).contains(userId)) {
            reactions.get(emoji).add(userId);
        }
    }

    public void removeReaction(String emoji, String userId) {
        if (reactions != null && reactions.containsKey(emoji)) {
            reactions.get(emoji).remove(userId);


            if (reactions.get(emoji).isEmpty()) {
                reactions.remove(emoji);
            }
        }
    }

    public boolean hasUserReacted(String emoji, String userId) {
        return reactions != null &&
                reactions.containsKey(emoji) &&
                reactions.get(emoji).contains(userId);
    }

    public int getTotalReactionCount() {
        int count = 0;
        if (reactions != null) {
            for (List<String> users : reactions.values()) {
                count += users.size();
            }
        }
        return count;
    }
}