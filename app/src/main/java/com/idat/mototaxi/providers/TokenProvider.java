package com.idat.mototaxi.providers;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.installations.FirebaseInstallations;
import com.idat.mototaxi.models.Token;

public class TokenProvider {

    DatabaseReference databaseReference;

    public TokenProvider() {
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Tokens");
    }

    public void create(final String id){
        /*
        FirebaseInstallations.getInstance().getId().addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                Token token = new Token(s);
                databaseReference.child(id).setValue(token);
            }
        });
        */

        //Este metodo esta obsoleto por ende el de arriba es lo nuevo supuestamente XD (Es mi suposicion)
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                Token token = new Token(instanceIdResult.getToken());
                databaseReference.child(id).setValue(token);
            }
        });
    }

    public DatabaseReference getToken(String idUser){
        return databaseReference.child(idUser);
    }
}
