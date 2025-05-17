package com.example.finalchatapp.services;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.finalchatapp.MainActivity;
import com.example.finalchatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NotificationService extends BroadcastReceiver {

    public static final String TAG = "NotificationService";
    public static final String CHANNEL_ID = "chat_notifications";
    public static final String CHECK_MESSAGES_ACTION = "com.example.finalchatapp.CHECK_MESSAGES";
    public static final int NOTIFICATION_REQUEST_CODE = 42;

    // Set to track message IDs that have already been processed
    private static final Set<String> processedMessageIds = new HashSet<>();
    private static long lastCheckTime = 0;
    // Flag to track if this is the first check after startup
    private static boolean isFirstCheck = true;
    // Stores users we've already fetched usernames for
    private static final Map<String, String> userCache = new HashMap<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "NotificationService receiver triggered with action: " +
                (intent.getAction() != null ? intent.getAction() : "null"));

        // Use a wake lock to ensure the device doesn't sleep during processing
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "FinalChatApp:NotificationWakeLock");

        try {
            wakeLock.acquire(60*1000L); // 60 seconds max

            checkForNewMessages(context);

            // Reschedule for next check
            scheduleNextAlarm(context);

        } catch (Exception e) {
            Log.e(TAG, "Error in notification service: " + e.getMessage(), e);
        } finally {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    // Schedule the next alarm immediately
    private void scheduleNextAlarm(Context context) {
        Log.d(TAG, "Scheduling next notification check");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationService.class);
        intent.setAction(CHECK_MESSAGES_ACTION);

        // Create a PendingIntent that won't be updated
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, NOTIFICATION_REQUEST_CODE, intent, flags);

        // Set the next alarm
        if (alarmManager != null) {
            long triggerTime = System.currentTimeMillis() + (30 * 1000); // 30 seconds

            // For Android 12+ (S and later)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                    );
                    Log.d(TAG, "Next notification check scheduled with setExactAndAllowWhileIdle for " + new Date(triggerTime));
                } else {
                    // Fall back to inexact alarm
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                    );
                    Log.d(TAG, "Permission for exact alarms not granted, using regular set for " + new Date(triggerTime));
                }
            }
            // For Android 6.0+ to 11 (Marshmallow to R)
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
                Log.d(TAG, "Next notification check scheduled with setExactAndAllowWhileIdle for " + new Date(triggerTime));
            } else {
                // For older Android versions
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
                Log.d(TAG, "Next notification check scheduled with set for " + new Date(triggerTime));
            }
        }
    }

    // Schedule initial notification checks
    public static void scheduleNotifications(Context context) {
        Log.d(TAG, "Starting initial notification schedule");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationService.class);
        intent.setAction(CHECK_MESSAGES_ACTION);

        // Create a PendingIntent that won't be updated
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, NOTIFICATION_REQUEST_CODE, intent, flags);

        // Cancel any existing alarms
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);

            // Schedule the first check in 5 seconds
            long triggerTime = System.currentTimeMillis() + 5000;

            // For Android 12+ (S and later)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                    );
                    Log.d(TAG, "Initial notification check scheduled with setExactAndAllowWhileIdle for " + new Date(triggerTime));
                } else {
                    // Fall back to inexact alarm
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                    );
                    Log.d(TAG, "Permission for exact alarms not granted, using regular set for " + new Date(triggerTime));
                }
            }
            // For Android 6.0+ to 11 (Marshmallow to R)
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
                Log.d(TAG, "Initial notification check scheduled with setExactAndAllowWhileIdle for " + new Date(triggerTime));
            } else {
                // For older Android versions
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
                Log.d(TAG, "Initial notification check scheduled with set for " + new Date(triggerTime));
            }

            // Create notification channel right away
            createNotificationChannel(context);

            Log.d(TAG, "Initial notification checks scheduled successfully");
        }
    }

    // Create notification channel (can be called multiple times safely)
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Chat Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for new chat messages");
            channel.enableLights(true);
            channel.enableVibration(true);

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            }
        }
    }

    // Check for new messages
    private void checkForNewMessages(Context context) {
        // Cleanup the processed message IDs if needed
        cleanupProcessedMessages();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "User not logged in, skipping notification check");
            return;
        }

        // If this is the first check after startup, don't show notifications
        // Just mark all current messages as processed
        if (isFirstCheck) {
            Log.d(TAG, "First check after startup - recording existing messages but not showing notifications");
            isFirstCheck = false;
            // Record existing messages but don't notify
            checkAllMessagesWithoutNotifying(context, currentUser.getUid());
            return;
        }

        Log.d(TAG, "Checking for new messages for user: " + currentUser.getUid());

        // Use a longer window for finding new messages (15 minutes)
        final long checkFromTime = System.currentTimeMillis() - (15 * 60 * 1000);

        // Check for new messages in each chat
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .collection("chats")
                .get()
                .addOnSuccessListener(chatSnapshots -> {
                    Log.d(TAG, "Found " + chatSnapshots.size() + " chats to check");

                    if (chatSnapshots.isEmpty()) {
                        Log.d(TAG, "No chats found for current user");
                        return;
                    }

                    for (QueryDocumentSnapshot chatDoc : chatSnapshots) {
                        String otherUserId = chatDoc.getId();
                        Log.d(TAG, "Checking messages from user: " + otherUserId);

                        // Check for messages from this user
                        checkChatMessages(context, otherUserId, currentUser.getUid(), checkFromTime);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking for chats: " + e.getMessage(), e);
                });
    }

    // Add this method to record existing messages without showing notifications
    private void checkAllMessagesWithoutNotifying(Context context, String currentUserId) {
        // Get all the user's chats
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .collection("chats")
                .get()
                .addOnSuccessListener(chatSnapshots -> {
                    Log.d(TAG, "Recording existing messages from " + chatSnapshots.size() + " chats");

                    for (QueryDocumentSnapshot chatDoc : chatSnapshots) {
                        String otherUserId = chatDoc.getId();

                        // Check both possible chat locations and just record the message IDs
                        recordExistingMessages(otherUserId, currentUserId);
                    }
                });
    }

    // Record existing message IDs so they won't trigger notifications
    private void recordExistingMessages(String otherUserId, String currentUserId) {
        // Combined ID approach
        String chatId;
        if (currentUserId.compareTo(otherUserId) < 0) {
            chatId = currentUserId + "_" + otherUserId;
        } else {
            chatId = otherUserId + "_" + currentUserId;
        }

        // Check with combined ID
        FirebaseFirestore.getInstance()
                .collection("chats")
                .document(chatId)
                .collection("messages")
                .get()
                .addOnSuccessListener(messages -> {
                    for (QueryDocumentSnapshot msgDoc : messages) {
                        processedMessageIds.add(msgDoc.getId());
                    }
                    Log.d(TAG, "Recorded " + messages.size() + " existing messages from combined ID");
                });
    }

    // Check messages in a specific chat
    private void checkChatMessages(Context context, String otherUserId, String currentUserId, long checkFromTime) {
        // Use a more precise timestamp: last check time or provided checkFromTime, whichever is more recent
        long effectiveCheckTime = Math.max(lastCheckTime, checkFromTime);

        // Update the last check time for the next run
        lastCheckTime = System.currentTimeMillis();

        Log.d(TAG, "Checking for messages newer than: " + new Date(effectiveCheckTime));

        // Create a chat ID using both user IDs
        String chatId;
        if (currentUserId.compareTo(otherUserId) < 0) {
            chatId = currentUserId + "_" + otherUserId;
        } else {
            chatId = otherUserId + "_" + currentUserId;
        }

        // Check for messages using the combined chat ID
        FirebaseFirestore.getInstance()
                .collection("chats")
                .document(chatId)
                .collection("messages")
                .get()
                .addOnSuccessListener(messages -> {
                    Log.d(TAG, "Found " + messages.size() + " total messages in chat");
                    processMessages(context, messages, otherUserId, currentUserId, effectiveCheckTime);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking messages with combined ID: " + e.getMessage());
                });
    }

    // Helper method to process messages from any location
    private void processMessages(Context context, QuerySnapshot messages,
                                 String otherUserId, String currentUserId, long checkFromTime) {
        // Skip if no messages
        if (messages.isEmpty()) return;

        // Find messages that qualify for notification
        List<QueryDocumentSnapshot> relevantMessages = new ArrayList<>();

        for (QueryDocumentSnapshot msgDoc : messages) {
            String messageId = msgDoc.getId();
            String senderId = msgDoc.getString("senderId");
            String receiverId = msgDoc.getString("receiverId");
            Long timestamp = msgDoc.getLong("timestamp");
            String content = msgDoc.getString("content");

            // Skip messages we've already processed
            if (processedMessageIds.contains(messageId)) {
                continue;
            }

            // If this message is FROM the other user TO the current user and is recent
            if (senderId != null && senderId.equals(otherUserId) &&
                    receiverId != null && receiverId.equals(currentUserId) &&
                    timestamp != null && timestamp > checkFromTime) {

                Log.d(TAG, "New message qualifies for notification: " + content);
                relevantMessages.add(msgDoc);

                // Add to processed set so we don't show it again
                processedMessageIds.add(messageId);
            }
        }

        // If we found new messages that qualify
        if (!relevantMessages.isEmpty()) {
            // Get the latest message
            QueryDocumentSnapshot latestMsg = relevantMessages.get(relevantMessages.size() - 1);
            String content = latestMsg.getString("content");

            // Check if we already have the username cached
            if (userCache.containsKey(otherUserId)) {
                String username = userCache.get(otherUserId);
                showNotification(context, username, content, otherUserId);
                return;
            }

            // Get the sender's username to show in the notification
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(otherUserId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        String username = userDoc.getString("username");
                        if (username == null) username = "User " + otherUserId.substring(0, 5);

                        // Cache the username for future use
                        userCache.put(otherUserId, username);

                        Log.d(TAG, "Showing notification from " + username + ": " + content);

                        // Show the notification
                        showNotification(context, username, content, otherUserId);
                    })
                    .addOnFailureListener(e -> {
                        // If we can't get the username, still show the notification
                        Log.e(TAG, "Error getting username: " + e.getMessage());
                        showNotification(context, "New message", content, otherUserId);
                    });
        } else {
            Log.d(TAG, "No new unprocessed messages found that qualify for notification");
        }
    }

    // Add this to your NotificationService class
    private void cleanupProcessedMessages() {
        // If we have too many processed messages, clear out older ones
        if (processedMessageIds.size() > 1000) {
            Log.d(TAG, "Cleaning up processed message IDs cache");
            processedMessageIds.clear();
        }
    }

    // Show a notification for a message
    private static void showNotification(Context context, String sender, String messageContent, String senderId) {
        Log.d(TAG, "Showing notification from " + sender + ": " + messageContent);

        // Create an intent for when notification is tapped
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("openChat", true);
        intent.putExtra("chatUserId", senderId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Create PendingIntent
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                senderId.hashCode(),
                intent,
                flags
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat)
                .setContentTitle(sender)
                .setContentText(messageContent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        // Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(senderId.hashCode(), builder.build());
            Log.d(TAG, "Notification displayed successfully");
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for notification: " + e.getMessage(), e);
        }
    }

    // Utility method to directly show a chat notification from outside
    public static void showDirectNotification(Context context, String sender, String message, String senderId) {
        // Create notification channel if needed
        createNotificationChannel(context);

        // Show the notification
        showNotification(context, sender, message, senderId);
    }

    // Clear a notification for a specific chat
    public static void clearNotification(Context context, String senderId) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(senderId.hashCode());
    }
}