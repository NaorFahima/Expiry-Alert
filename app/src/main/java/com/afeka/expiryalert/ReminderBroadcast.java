package com.afeka.expiryalert;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.util.Log;
import android.widget.RemoteViews;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class ReminderBroadcast extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("BCAST", Integer.toString(intent.getIntExtra("ID", 1)));
        Log.d("BCAST", intent.getStringExtra("Type"));

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        String type = intent.getStringExtra("Type");
        int id = intent.getIntExtra("ID", 1);

        if(type.equals("Create") || type.equals("Update")) {
            String title = intent.getStringExtra("Title");
            int numDaysLeft = intent.getIntExtra("numDaysLeft", 0);
            int expiration = R.string.notification_item_expires_in;
            String daysReminderColor;
            if (numDaysLeft <= 3) {
                daysReminderColor = "red";
                if (numDaysLeft <= 0) {
                    expiration = R.string.notification_item_expired;
                }
            } else {
                daysReminderColor = "black";
            }
            RemoteViews notificationLayout = new RemoteViews(context.getPackageName(), R.layout.custom_notification_small);
            notificationLayout.setTextViewText(R.id.notification_title, title);
            notificationLayout.setTextViewText(R.id.notification_text, Html.fromHtml(context.getString(expiration,
                    numDaysLeft, daysReminderColor)));
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, "notify")
                    .setSmallIcon(R.drawable.favicon)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setCustomContentView(notificationLayout);
            notificationManager.notify(id, mBuilder.build());
        } else if(type.equals("Delete")) {
            notificationManager.cancel(id);
        }
    }
}
