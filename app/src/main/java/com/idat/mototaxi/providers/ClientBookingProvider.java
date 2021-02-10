package com.idat.mototaxi.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.idat.mototaxi.models.ClientBooking;

import java.util.HashMap;
import java.util.Map;

public class ClientBookingProvider {

    DatabaseReference databaseReference;

    public ClientBookingProvider() {
        databaseReference = FirebaseDatabase.getInstance().getReference().child("ClientBooking");
    }

    public Task<Void> create(ClientBooking clientBooking){
        return databaseReference.child(clientBooking.getIdClient()).setValue(clientBooking);
    }

    public Task<Void> updateStatus(String idClientBooking, String status){
        Map<String , Object> map = new HashMap<>();
        map.put("status", status);
        return  databaseReference.child(idClientBooking).updateChildren(map);
    }

    public Task<Void> updateIdHistoryBooking(String idClientBooking){
        String idPush = databaseReference.push().getKey();
        Map<String , Object> map = new HashMap<>();
        map.put("idHistoryBooking", idPush);
        return  databaseReference.child(idClientBooking).updateChildren(map);
    }

    public Task<Void> updatePrice(String idClientBooking, double price){
        Map<String , Object> map = new HashMap<>();
        map.put("price", price);
        return  databaseReference.child(idClientBooking).updateChildren(map);
    }


    public DatabaseReference getStatus(String idClientBooking){
        return databaseReference.child(idClientBooking).child("status");
    }

    public DatabaseReference getClientBooking(String idClientBooking){
        return databaseReference.child(idClientBooking);
    }

    public Task<Void> delete(String idClientBooking){
        return databaseReference.child(idClientBooking).removeValue();
    }
}
