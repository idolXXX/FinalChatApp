package com.example.finalchatapp.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

/**
 * Receiver to start notification services when the device boots up
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "BootReceiver triggered with action: " + (intent.getAction() != null ? intent.getAction() : "null"));

        if (intent.getAction() != null &&
                intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG, "Boot completed - initializing notification services");


            new Handler(Looper.getMainLooper()).postDelayed(() -> {

                NotificationService.scheduleNotifications(context);


                MessageListener.startMessageListeners(context);

                Log.d(TAG, "Notification services initialized after boot");
            }, 5000);
        }
    }
}