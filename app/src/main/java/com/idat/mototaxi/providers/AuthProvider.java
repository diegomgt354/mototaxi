package com.idat.mototaxi.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class AuthProvider {// se encarga de la autentificacion

    FirebaseAuth auth;

    public AuthProvider() {
        auth = FirebaseAuth.getInstance();
    }

    public Task<AuthResult> register(String email, String password){
        return auth.createUserWithEmailAndPassword(email,password);
    }

    public Task<AuthResult> login(String email, String password){
        return auth.signInWithEmailAndPassword(email,password);
    }

    public String getId(){
        return auth.getCurrentUser().getUid();
    }

    public boolean existSession(){
        boolean exist = false;
        if(auth.getCurrentUser() != null){
            exist = true;
        }
        return exist;
    }

    public void logout(){
        auth.signOut();
    }
}
