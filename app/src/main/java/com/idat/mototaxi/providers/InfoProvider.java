package com.idat.mototaxi.providers;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class InfoProvider {

    DatabaseReference databaseReference;

    public InfoProvider() {
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Info");
    }

    public DatabaseReference getInfo(){
        return  databaseReference;
    }
}
