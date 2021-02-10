package com.idat.mototaxi.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.idat.mototaxi.providers.ClientBookingProvider;

public class CancelReceiver extends BroadcastReceiver  {
    private ClientBookingProvider clientBookingProvider;

    @Override
    public void onReceive(Context context, Intent intent) {
        String idClient = intent.getExtras().getString("idClient");
        clientBookingProvider = new ClientBookingProvider();
        clientBookingProvider.updateStatus(idClient, "cancel");

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(2);
    }
}
