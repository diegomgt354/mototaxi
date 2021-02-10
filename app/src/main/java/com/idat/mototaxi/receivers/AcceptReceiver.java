package com.idat.mototaxi.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.idat.mototaxi.activities.driver.MapDriverBookingActivity;
import com.idat.mototaxi.providers.AuthProvider;
import com.idat.mototaxi.providers.ClientBookingProvider;
import com.idat.mototaxi.providers.GeofireProvider;

public class AcceptReceiver extends BroadcastReceiver {

    private ClientBookingProvider clientBookingProvider;
    private GeofireProvider geofireProvider;
    private AuthProvider authProvider;

    @Override
    public void onReceive(Context context, Intent intent) {

        authProvider = new AuthProvider();
        geofireProvider = new GeofireProvider("active_drivers");
        geofireProvider.removeLocation(authProvider.getId());

        String idClient = intent.getExtras().getString("idClient");
        clientBookingProvider = new ClientBookingProvider();
        clientBookingProvider.updateStatus(idClient, "accept");

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(2);

        Intent intent1 = new Intent(context, MapDriverBookingActivity.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent1.setAction(Intent.ACTION_RUN);
        intent1.putExtra("idClient", idClient);
        context.startActivity(intent1);
    }
}
