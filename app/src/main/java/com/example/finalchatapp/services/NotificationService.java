package com.example.finalchatapp.services;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.finalchatapp.MainActivity;
import com.example.finalchatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class NotificationService extends BroadcastReceiver {

    private static final String CHANNEL_ID = "chat_notification_channel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        checkForNewMessages(context);
    }

    // Method to start the notification service (call this from the MainActivity)
    public static void scheduleNotifications(Context context) {
        // Create an alarm that repeats every minute to check for new messages
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationService.class);

        // Use FLAG_IMMUTABLE for Android 12+
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                flags
        );

        // Set up repeating alarm to check every minute
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                60 * 1000, // 1 minute
                pendingIntent
        );
    }

    // Method to check for new messages
    private void checkForNewMessages(Context context) {
        // Create notification channel for Android 8.0+
        createNotificationChannel(context);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Check for messages received in the last minute
        long oneMinuteAgo = System.currentTimeMillis() - (60 * 1000);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .collection("chats")
                .whereGreaterThan("lastMessageTimestamp", oneMinuteAgo)
                .whereNotEqualTo("lastMessageSenderId", currentUser.getUid()) // Only messages from others
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Get the sender ID of the last message
                        String senderId = queryDocumentSnapshots.getDocuments().get(0).getString("lastMessageSenderId");
                        String message = queryDocumentSnapshots.getDocuments().get(0).getString("lastMessageContent");

                        // Get the sender's name
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(senderId)
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    String senderName = documentSnapshot.getString("username");
                                    if (senderName != null && message != null) {
                                        // Show notification
                                        showNotification(context, senderName, message);
                                    }
                                });
                    }
                });
    }

    // Method to show notification
    private void showNotification(Context context, String sender, String message) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create an intent to open the app when notification is clicked
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Use FLAG_IMMUTABLE for Android 12+
        int flags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                flags
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat)
                .setContentTitle(sender)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Show notification
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    // Create notification channel for Android 8.0+
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Chat Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for new chat messages");

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}