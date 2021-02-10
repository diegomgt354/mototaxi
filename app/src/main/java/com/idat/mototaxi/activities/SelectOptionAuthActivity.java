package com.idat.mototaxi.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.idat.mototaxi.R;
import com.idat.mototaxi.activities.client.RegisterActivity;
import com.idat.mototaxi.activities.driver.RegisterDriverActivity;
import com.idat.mototaxi.includes.MyToolbar;

public class SelectOptionAuthActivity extends AppCompatActivity {

    Button btnLogin;
    Button btnRegister;

    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_option_auth);

        MyToolbar.show(this,"Seleccionar Opcion",true);

        preferences = getApplicationContext().getSharedPreferences("typeUser",MODE_PRIVATE);

        btnLogin = findViewById(R.id.btnGoToSelectLogin);
        btnRegister = findViewById(R.id.btnGoToSelectRegister);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToLogin();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToRegister();
            }
        });
    }

    void goToLogin() {
        Intent intent = new Intent(SelectOptionAuthActivity.this,LoginActivity.class);
        startActivity(intent);
    }

    void goToRegister() {
        String typeUser = preferences.getString("user","");
        if(typeUser.equals("client")){
            Intent intent = new Intent(SelectOptionAuthActivity.this, RegisterActivity.class);
            startActivity(intent);
        }else if(typeUser.equals("driver")){
            Intent intent = new Intent(SelectOptionAuthActivity.this, RegisterDriverActivity.class);
            startActivity(intent);
        }

    }
}