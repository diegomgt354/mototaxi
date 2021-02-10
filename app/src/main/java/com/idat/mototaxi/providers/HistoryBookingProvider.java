package com.idat.mototaxi.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.idat.mototaxi.models.ClientBooking;
import com.idat.mototaxi.models.HistoryBooking;

import java.util.HashMap;
import java.util.Map;

public class HistoryBookingProvider {

    DatabaseReference databaseReference;

    public HistoryBookingProvider() {
        databaseReference = FirebaseDatabase.getInstance().getReference().child("HistoryBooking");
    }

    public Task<Void> create(HistoryBooking historyBooking){
        return databaseReference.child(historyBooking.getIdHistoryBooking()).setValue(historyBooking);
    }

    public Task<Void> updateCalificationClient(String idHistoryBooking, float calificationClient){
        Map<String, Object> map = new HashMap<>();
        map.put("calificationClient", calificationClient);
        return databaseReference.child(idHistoryBooking).updateChildren(map);
    }

    public Task<Void> updateCalificationDriver(String idHistoryBooking, float calificationDriver){
        Map<String, Object> map = new HashMap<>();
        map.put("calificationDriver", calificationDriver);
        return databaseReference.child(idHistoryBooking).updateChildren(map);
    }

    public DatabaseReference getHistoryBooking(String idHistoryBooking){
        return databaseReference.child(idHistoryBooking);
    }


}
