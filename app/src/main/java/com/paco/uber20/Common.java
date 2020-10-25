package com.paco.uber20;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.paco.uber20.Model.driverModel;
import com.paco.uber20.Model.riderModel;
import com.paco.uber20.Services.MessagingServices;

public class Common {
    public static final String USERS_INFO_REFERENCE = "Users";
    public static final String DRIVER_INFO_REFERENCE = "Users/DriverInfo";
    public static final String RIDER_INFO_REFERENCE = "Users/Riders";
    public static final String TOKEN_REFERENCE = "Token";
    public static final String NT_TITLE = "title";
    public static final String NT_CONTENT = "body";
    public static final String DRIVERS_LOCATION_REFERENCES = "Users/DriversLocation";
    public static driverModel currentDriver;
    public static riderModel currentUser;
    public static String buildWelcomeMessage() {
        if(Common.currentDriver!=null){
            return new StringBuilder("Welcome ")
                    .append("")
                    .append(Common.currentDriver.getName()).toString();
        }else{
            return "";
        }
    }

    public static void showNotification(Context context, int id, String title, String body,Intent i) {
        PendingIntent pendingIntent = null;
        if(i !=null){
            pendingIntent = PendingIntent.getActivity(context,id,i, PendingIntent.FLAG_UPDATE_CURRENT);
            String NOTIFICATION_CHANNEL_ID = "com.paco.uber20";
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
            {
                NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                        "uber 20",
                        NotificationManager.IMPORTANCE_HIGH);
                notificationChannel.setDescription("UBER 20");
                notificationChannel.enableLights(true);
                notificationChannel.enableLights(true);
                notificationChannel.setVibrationPattern(new long[]{0,1000,500,1000});
                notificationChannel.enableVibration(true);
                notificationManager.createNotificationChannel(notificationChannel);
            }
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
            builder.setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(false)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(Notification.DEFAULT_VIBRATE)
                    .setSmallIcon(R.drawable.ic_car_1)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                            R.drawable.ic_car_1));
            if(pendingIntent !=null){
                builder.setContentIntent(pendingIntent);
            }
            Notification notification = builder.build();
            notificationManager.notify(id, notification);
        }
    }
}
