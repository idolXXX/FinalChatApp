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


    private static final Set<String> processedMessageIds = new HashSet<>();
    private static long lastCheckTime = 0;

    private static boolean isFirstCheck = true;

    private static final Map<String, String> userCache = new HashMap<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "NotificationService receiver triggered with action: " +
                (intent.getAction() != null ? intent.getAction() : "null"));


        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "FinalChatApp:NotificationWakeLock");

        try {
            wakeLock.acquire(60*1000L); // 60 seconds max

            checkForNewMessages(context);


            scheduleNextAlarm(context);

        } catch (Exception e) {
            Log.e(TAG, "Error in notification service: " + e.getMessage(), e);
        } finally {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }


    private void scheduleNextAlarm(Context context) {
        Log.d(TAG, "Scheduling next notification check");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationService.class);
        intent.setAction(CHECK_MESSAGES_ACTION);


        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, NOTIFICATION_REQUEST_CODE, intent, flags);


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

                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
                Log.d(TAG, "Next notification check scheduled with set for " + new Date(triggerTime));
            }
        }
    }


    public static void scheduleNotifications(Context context) {
        Log.d(TAG, "Starting initial notification schedule");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationService.class);
        intent.setAction(CHECK_MESSAGES_ACTION);


        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, NOTIFICATION_REQUEST_CODE, intent, flags);


        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);


            long triggerTime = System.currentTimeMillis() + 5000;


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                    );
                    Log.d(TAG, "Initial notification check scheduled with setExactAndAllowWhileIdle for " + new Date(triggerTime));
                } else {

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

                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
                Log.d(TAG, "Initial notification check scheduled with set for " + new Date(triggerTime));
            }


            createNotificationChannel(context);

            Log.d(TAG, "Initial notification checks scheduled successfully");
        }
    }


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


    private void checkForNewMessages(Context context) {

        cleanupProcessedMessages();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "User not logged in, skipping notification check");
            return;
        }


        if (isFirstCheck) {
            Log.d(TAG, "First check after startup - recording existing messages but not showing notifications");
            isFirstCheck = false;

            checkAllMessagesWithoutNotifying(context, currentUser.getUid());
            return;
        }

        Log.d(TAG, "Checking for new messages for user: " + currentUser.getUid());


        final long checkFromTime = System.currentTimeMillis() - (15 * 60 * 1000);


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


                        checkChatMessages(context, otherUserId, currentUser.getUid(), checkFromTime);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking for chats: " + e.getMessage(), e);
                });
    }


    private void checkAllMessagesWithoutNotifying(Context context, String currentUserId) {

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .collection("chats")
                .get()
                .addOnSuccessListener(chatSnapshots -> {
                    Log.d(TAG, "Recording existing messages from " + chatSnapshots.size() + " chats");

                    for (QueryDocumentSnapshot chatDoc : chatSnapshots) {
                        String otherUserId = chatDoc.getId();


                        recordExistingMessages(otherUserId, currentUserId);
                    }
                });
    }


    private void recordExistingMessages(String otherUserId, String currentUserId) {

        String chatId;
        if (currentUserId.compareTo(otherUserId) < 0) {
            chatId = currentUserId + "_" + otherUserId;
        } else {
            chatId = otherUserId + "_" + currentUserId;
        }


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


    private void checkChatMessages(Context context, String otherUserId, String currentUserId, long checkFromTime) {

        long effectiveCheckTime = Math.max(lastCheckTime, checkFromTime);


        lastCheckTime = System.currentTimeMillis();

        Log.d(TAG, "Checking for messages newer than: " + new Date(effectiveCheckTime));


        String chatId;
        if (currentUserId.compareTo(otherUserId) < 0) {
            chatId = currentUserId + "_" + otherUserId;
        } else {
            chatId = otherUserId + "_" + currentUserId;
        }


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


    private void processMessages(Context context, QuerySnapshot messages,
                                 String otherUserId, String currentUserId, long checkFromTime) {

        if (messages.isEmpty()) return;


        List<QueryDocumentSnapshot> relevantMessages = new ArrayList<>();

        for (QueryDocumentSnapshot msgDoc : messages) {
            String messageId = msgDoc.getId();
            String senderId = msgDoc.getString("senderId");
            String receiverId = msgDoc.getString("receiverId");
            Long timestamp = msgDoc.getLong("timestamp");
            String content = msgDoc.getString("content");


            if (processedMessageIds.contains(messageId)) {
                continue;
            }


            if (senderId != null && senderId.equals(otherUserId) &&
                    receiverId != null && receiverId.equals(currentUserId) &&
                    timestamp != null && timestamp > checkFromTime) {

                Log.d(TAG, "New message qualifies for notification: " + content);
                relevantMessages.add(msgDoc);


                processedMessageIds.add(messageId);
            }
        }


        if (!relevantMessages.isEmpty()) {

            QueryDocumentSnapshot latestMsg = relevantMessages.get(relevantMessages.size() - 1);
            String content = latestMsg.getString("content");

            if (userCache.containsKey(otherUserId)) {
                String username = userCache.get(otherUserId);
                showNotification(context, username, content, otherUserId);
                return;
            }


            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(otherUserId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        String username = userDoc.getString("username");
                        if (username == null) username = "User " + otherUserId.substring(0, 5);


                        userCache.put(otherUserId, username);

                        Log.d(TAG, "Showing notification from " + username + ": " + content);


                        showNotification(context, username, content, otherUserId);
                    })
                    .addOnFailureListener(e -> {

                        Log.e(TAG, "Error getting username: " + e.getMessage());
                        showNotification(context, "New message", content, otherUserId);
                    });
        } else {
            Log.d(TAG, "No new unprocessed messages found that qualify for notification");
        }
    }


    private void cleanupProcessedMessages() {

        if (processedMessageIds.size() > 1000) {
            Log.d(TAG, "Cleaning up processed message IDs cache");
            processedMessageIds.clear();
        }
    }


    private static void showNotification(Context context, String sender, String messageContent, String senderId) {
        Log.d(TAG, "Showing notification from " + sender + ": " + messageContent);


        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("openChat", true);
        intent.putExtra("chatUserId", senderId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);


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


        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat)
                .setContentTitle(sender)
                .setContentText(messageContent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);


        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(senderId.hashCode(), builder.build());
            Log.d(TAG, "Notification displayed successfully");
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for notification: " + e.getMessage(), e);
        }
    }


    public static void showDirectNotification(Context context, String sender, String message, String senderId) {

        createNotificationChannel(context);


        showNotification(context, sender, message, senderId);
    }


    public static void clearNotification(Context context, String senderId) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(senderId.hashCode());
    }
}