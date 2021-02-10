package com.idat.mototaxi.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.idat.mototaxi.R;
import com.idat.mototaxi.activities.client.MapClientActivity;
import com.idat.mototaxi.activities.driver.MapDriverActivity;

public class MainActivity extends AppCompatActivity {

    Button btnSelectClient;
    Button btnSelectDriver;

    SharedPreferences preferences;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getApplicationContext().getSharedPreferences("typeUser",MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        auth = FirebaseAuth.getInstance();

        btnSelectDriver = findViewById(R.id.btnSelectConductor);
        btnSelectClient = findViewById(R.id.btnSelectCliente);

        btnSelectDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putString("user","driver");
                editor.apply();
                goToSelectAuth();
            }
        });

        btnSelectClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putString("user","client");
                editor.apply();
                goToSelectAuth();
            }
        });
    }

    @Override
    protected void onStart() {//metodo del ciclo de vida de android
        super.onStart();

        if(auth.getCurrentUser()!=null){
            String typeUser = preferences.getString("user","");
            if(typeUser.equals("client")){
                Intent intent = new Intent(MainActivity.this, MapClientActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);// para que el usuario ya no pueda ir hacia atras
                startActivity(intent);
            }else if(typeUser.equals("driver")){
                Intent intent = new Intent(MainActivity.this, MapDriverActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);// para que el usuario ya no pueda ir hacia atras
                startActivity(intent);
            }
        }
    }

    void goToSelectAuth() {
        Intent intent = new Intent(MainActivity.this,SelectOptionAuthActivity.class);
        startActivity(intent);
    }
}