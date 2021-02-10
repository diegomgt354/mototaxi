package com.idat.mototaxi.channel;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.ColorInt;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.idat.mototaxi.R;

public class NotificationHelper extends ContextWrapper {

    private static final String CHANNEL_ID = "com.idat.mototaxi";
    private static final String CHANNEL_NAME = "Mototaxi";

    private NotificationManager manager;

    public NotificationHelper(Context base) {
        super(base);
        //Verificando la version del SO
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createChannel();
        }

    }

    //Creando la base de la notificacion
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannel(){
        NotificationChannel notificationChannel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
                );
        notificationChannel.enableLights(true);
        notificationChannel.enableVibration(true);
        notificationChannel.setLightColor(Color.GRAY);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        getManager().createNotificationChannel(notificationChannel);
    }

    public NotificationManager getManager(){
        if(manager == null){
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }

    //Creando el contexto de la notificacion
    @RequiresApi(api = Build.VERSION_CODES.O)
    public Notification.Builder getNotification(String title, String body, PendingIntent intent, Uri soundUri){
        return new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setContentIntent(intent)
                .setSmallIcon(R.drawable.ic_mototaxi)
                .setStyle(new Notification.BigTextStyle()
                    .bigText(body).setBigContentTitle(title)
                );
    }

    //Versiones anteriores
    public NotificationCompat.Builder getNotificationOldAPI(String title, String body, PendingIntent intent, Uri soundUri){
        return new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setContentIntent(intent)
                .setSmallIcon(R.drawable.ic_mototaxi)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(body).setBigContentTitle(title)
                );
    }

    //Metodos para aceptar o cancelar el pedido del cliente mediante la notificacion

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Notification.Builder getNotificationActions(String title, String body, Uri soundUri, Notification.Action acceptAction, Notification.Action cancelAction){
        return new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setSmallIcon(R.drawable.ic_mototaxi)
                .addAction(acceptAction)
                .addAction(cancelAction)
                .setStyle(new Notification.BigTextStyle()
                        .bigText(body).setBigContentTitle(title)
                );
    }

    public NotificationCompat.Builder getNotificationOldAPIActions(String title, String body, Uri soundUri, NotificationCompat.Action acceptAction, NotificationCompat.Action cancelAction){
        return new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setSmallIcon(R.drawable.ic_mototaxi)
                .addAction(acceptAction)
                .addAction(cancelAction)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(body).setBigContentTitle(title)
                );
    }
}
