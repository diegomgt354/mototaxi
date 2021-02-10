package com.idat.mototaxi.activities.client;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.idat.mototaxi.R;
import com.idat.mototaxi.activities.driver.MapDriverActivity;
import com.idat.mototaxi.activities.driver.RegisterDriverActivity;
import com.idat.mototaxi.includes.MyToolbar;
import com.idat.mototaxi.models.Client;
import com.idat.mototaxi.models.Users;
import com.idat.mototaxi.providers.AuthProvider;
import com.idat.mototaxi.providers.ClientProvider;

import dmax.dialog.SpotsDialog;

public class RegisterActivity extends AppCompatActivity {

    AuthProvider authProvider;
    ClientProvider clientProvider;

    TextInputEditText nameTxt;
    TextInputEditText emailTxt;
    TextInputEditText passwordTxt;
    Button btnRegisterClient;

    AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        MyToolbar.show(this,"Registrar Cliente",true);

        authProvider = new AuthProvider();
        clientProvider = new ClientProvider();

        dialog = new SpotsDialog.Builder().setContext(RegisterActivity.this).setMessage("Cargando...").build();

        nameTxt = findViewById(R.id.txtClientNombre);
        emailTxt = findViewById(R.id.txtClientCorreo);
        passwordTxt = findViewById(R.id.txtClientPasswrd);

        btnRegisterClient = findViewById(R.id.btnClientRegister);

        btnRegisterClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickRegister();
            }
        });

    }

    void clickRegister() {
        String name = nameTxt.getText().toString();
        String email = emailTxt.getText().toString();
        String pass = passwordTxt.getText().toString();

        if(!name.isEmpty() && !email.isEmpty() && !pass.isEmpty()){
            if(pass.length()>=6){
                register(name,email,pass);
            }else{
                Toast.makeText(RegisterActivity.this,"La contraseña debe de tener minimo 6 caracteres",Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(RegisterActivity.this,"Los campos de Usuario y Password no deben d estar vacios",Toast.LENGTH_SHORT).show();
        }

    }

    void register(String name,String email, String pass) {
        dialog.show();
        authProvider.register(email,pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    String id = FirebaseAuth.getInstance().getCurrentUser().getUid();//obtiene el identificador de usuarios que se avaba de registrar
                    Client client = new Client(id,name,email);
                    create(client);
                    //saveUser(id,name,email);
                }else{
                    Toast.makeText(RegisterActivity.this,"No se pudo generar el Registro",Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            }
        });
    }

    void create(Client client){
        clientProvider.create(client).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(RegisterActivity.this,"El registro se genero de manera existosa",Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, MapClientActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);// para que el usuario ya no pueda ir hacia atras
                    startActivity(intent);
                }else{
                    Toast.makeText(RegisterActivity.this,"No se pudo generar el Registro",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
/*
    void saveUser(String id,String name,String email) {
        String selectUser = preferences.getString("user","");
        Users user = new Users();
        user.setEmail(email);
        user.setName(name);
        if(selectUser.equals("driver")){
            database.child("Users").child("Drivers").child(id).setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        Toast.makeText(RegisterActivity.this,"Se genero el registro de conductor",Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }else if(selectUser.equals("client")){
            database.child("Users").child("Clients").child(id).setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    Toast.makeText(RegisterActivity.this,"Se genero el registro de cliente",Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
*/
}