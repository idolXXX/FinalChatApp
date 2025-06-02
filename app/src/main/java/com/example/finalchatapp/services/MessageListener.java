package com.example.finalchatapp.services;

import android.content.Context;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to listen for incoming messages and show notifications in real-time
 * Improved to prevent memory leaks and handle concurrent access safely
 */
public class MessageListener {
    private static final String TAG = "MessageListener";

    // Use ConcurrentHashMap for thread safety
    private static final Map<String, ListenerRegistration> activeListeners = new ConcurrentHashMap<>();


    private static final Set<String> processedMessageIds = Collections.synchronizedSet(new HashSet<>());

    // Cache user names to avoid frequent Firestore queries (thread-safe)
    private static final Map<String, String> userCache = new ConcurrentHashMap<>();

    // Use WeakReference to prevent memory leaks
    private static WeakReference<Context> contextRef;

    // Maximum size for processed message IDs cache
    private static final int MAX_PROCESSED_IDS = 1000;

    // Flag to track if listeners are already running
    private static boolean isRunning = false;


    public static synchronized void startMessageListeners(@NonNull Context context) {

        contextRef = new WeakReference<>(context.getApplicationContext());


        if (isRunning) {
            Log.d(TAG, "Message listeners already running, skipping initialization");
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "Cannot start listeners: User not logged in");
            return;
        }

        String currentUserId = currentUser.getUid();
        Log.d(TAG, "Starting message listeners for user: " + currentUserId);


        isRunning = true;


        cleanupProcessedMessageIds();


        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .collection("chats")
                .get()
                .addOnSuccessListener(chatSnapshots -> {

                    Context appContext = contextRef.get();
                    if (appContext == null) {
                        Log.e(TAG, "Context no longer available, canceling setup");
                        cleanup();
                        return;
                    }


                    for (DocumentSnapshot chatDoc : chatSnapshots.getDocuments()) {
                        String otherUserId = chatDoc.getId();
                        setupChatListener(appContext, currentUserId, otherUserId);
                    }


                    setupNewChatsListener(appContext, currentUserId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user's chats: " + e.getMessage());

                    isRunning = false;
                });
    }

    /**
     * Listen for new chats being created
     */
    private static void setupNewChatsListener(@NonNull Context context, String currentUserId) {

        if (activeListeners.containsKey("new_chats")) {
            Log.d(TAG, "New chats listener already active");
            return;
        }

        Log.d(TAG, "Setting up new chats listener");


        ListenerRegistration registration = FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .collection("chats")
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshots, e) -> {

                    Context appContext = contextRef.get();
                    if (appContext == null) {
                        Log.e(TAG, "Context no longer available, removing listener");
                        ListenerRegistration reg = activeListeners.remove("new_chats");
                        if (reg != null) reg.remove();
                        return;
                    }

                    if (e != null) {
                        Log.e(TAG, "Listen for new chats failed: ", e);
                        return;
                    }

                    if (snapshots == null) return;


                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            String otherUserId = dc.getDocument().getId();
                            Log.d(TAG, "New chat detected with user: " + otherUserId);


                            setupChatListener(appContext, currentUserId, otherUserId);
                        }
                    }
                });


        activeListeners.put("new_chats", registration);
    }

    /**
     * Setup a listener for messages in a specific chat
     */
    private static void setupChatListener(@NonNull Context context, String currentUserId, String otherUserId) {

        String listenerKey = "chat_" + currentUserId + "_" + otherUserId;


        if (activeListeners.containsKey(listenerKey)) {
            Log.d(TAG, "Chat listener already exists for: " + otherUserId);
            return;
        }

        Log.d(TAG, "Setting up message listener for chat with: " + otherUserId);


        String chatId;
        if (currentUserId.compareTo(otherUserId) < 0) {
            chatId = currentUserId + "_" + otherUserId;
        } else {
            chatId = otherUserId + "_" + currentUserId;
        }


        ListenerRegistration registration = FirebaseFirestore.getInstance()
                .collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshots, e) -> {

                    Context appContext = contextRef.get();
                    if (appContext == null) {
                        Log.e(TAG, "Context no longer available, removing listener");
                        ListenerRegistration reg = activeListeners.remove(listenerKey);
                        if (reg != null) reg.remove();
                        return;
                    }

                    if (e != null) {
                        Log.e(TAG, "Listen for messages failed: ", e);
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        return;
                    }


                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {

                            String messageId = dc.getDocument().getId();


                            synchronized (processedMessageIds) {
                                if (processedMessageIds.contains(messageId)) {
                                    continue;
                                }// Add to processed set
                                processedMessageIds.add(messageId);
                            }

                            String senderId = dc.getDocument().getString("senderId");
                            String receiverId = dc.getDocument().getString("receiverId");
                            String content = dc.getDocument().getString("content");


                            cleanupProcessedMessageIds();


                            if (senderId != null && senderId.equals(otherUserId) &&
                                    receiverId != null && receiverId.equals(currentUserId)) {

                                Log.d(TAG, "New message detected from " + otherUserId + ": " + content);


                                String cachedName = userCache.get(otherUserId);
                                if (cachedName != null) {

                                    NotificationService.showDirectNotification(appContext, cachedName, content, otherUserId);
                                } else {

                                    FirebaseFirestore.getInstance()
                                            .collection("users")
                                            .document(otherUserId)
                                            .get()
                                            .addOnSuccessListener(userDoc -> {

                                                Context ctx = contextRef.get();
                                                if (ctx == null) return;

                                                String username = userDoc.getString("username");
                                                if (username == null) username = "User " + otherUserId.substring(0, Math.min(5, otherUserId.length()));


                                                userCache.put(otherUserId, username);


                                                NotificationService.showDirectNotification(ctx, username, content, otherUserId);
                                            })
                                            .addOnFailureListener(fetchError -> {

                                                Context ctx = contextRef.get();
                                                if (ctx == null) return;


                                                NotificationService.showDirectNotification(ctx, "New message", content, otherUserId);
                                            });
                                }
                            }
                        }
                    }
                });


        activeListeners.put(listenerKey, registration);
    }

    /**
     * Clean up the processed message IDs cache if it's too large
     */
    private static void cleanupProcessedMessageIds() {
        synchronized (processedMessageIds) {
            if (processedMessageIds.size() > MAX_PROCESSED_IDS) {
                Log.d(TAG, "Cleaning up processed message IDs cache: " + processedMessageIds.size() + " items");
                processedMessageIds.clear();
            }
        }
    }

    /**
     * Stop all active message listeners
     */
    public static synchronized void stopAllListeners() {
        Log.d(TAG, "Stopping all message listeners: " + activeListeners.size());

        // Store registrations in a temp list to avoid ConcurrentModificationException
        for (ListenerRegistration registration : activeListeners.values()) {
            try {
                registration.remove();
            } catch (Exception e) {
                Log.e(TAG, "Error removing listener: " + e.getMessage());
            }
        }

        activeListeners.clear();
        isRunning = false;
    }

    /**
     * Clean up resources to prevent memory leaks
     */
    public static synchronized void cleanup() {
        stopAllListeners();

        synchronized (processedMessageIds) {
            processedMessageIds.clear();
        }

        userCache.clear();

        if (contextRef != null) {
            contextRef.clear();
            contextRef = null;
        }

        isRunning = false;

        Log.d(TAG, "MessageListener resources cleaned up");
    }

    /**
     * Check if message listeners are currently running
     */
    public static boolean isListening() {
        return isRunning && !activeListeners.isEmpty();
    }

    /**
     * Restart listeners if they're not running
     * Safe to call from any context to ensure listeners are active
     */
    public static void ensureListenersActive(@NonNull Context context) {
        // Run on main thread to avoid Firebase errors
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!isRunning || activeListeners.isEmpty()) {
                Log.d(TAG, "Restarting message listeners");
                stopAllListeners(); // Clean up any stale listeners
                startMessageListeners(context);
            }
        });
    }
}