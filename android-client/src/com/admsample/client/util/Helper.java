package com.admsample.client.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.admsample.client.R;

public class Helper {
    public static void sendNotification(Context context, int notificationId,
            String title, String content) {
        // Show a notification.
        final NotificationManager nMgr = (NotificationManager) context.getSystemService(
                context.NOTIFICATION_SERVICE);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(), 0);
        Notification notif = new Notification(R.drawable.icon, title, 
                System.currentTimeMillis());
        notif.setLatestEventInfo(context, title, (content == null) ? "" : content, contentIntent);
        nMgr.notify(notificationId, notif);
    }
}
