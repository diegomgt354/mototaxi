package com.idat.mototaxi.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.idat.mototaxi.models.Client;
import com.idat.mototaxi.models.Driver;

import java.util.HashMap;
import java.util.Map;

public class DriverProvider {
    DatabaseReference databaseReference;

    public DriverProvider() {
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers");
    }

    public Task<Void> create(Driver driver){
        Map<String, Object> map = new HashMap<>();
        map.put("name",driver.getName());
        map.put("email",driver.getEmail());
        map.put("marca",driver.getMarca());
        map.put("placa",driver.getPlaca());
        return databaseReference.child(driver.getId()).setValue(map);
    }

    public Task<Void> update(Driver driver){
        Map<String, Object> map = new HashMap<>();
        map.put("name",driver.getName());
        map.put("image",driver.getImage());
        map.put("marca",driver.getMarca());
        map.put("placa",driver.getPlaca());
        return databaseReference.child(driver.getId()).updateChildren(map);
    }

    public DatabaseReference getDriver(String idDriver){
        return databaseReference.child(idDriver);
    }
}
