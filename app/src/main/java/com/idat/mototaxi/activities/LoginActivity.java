package com.idat.mototaxi.activities;

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
import com.google.firebase.database.FirebaseDatabase;
import com.idat.mototaxi.R;
import com.idat.mototaxi.activities.client.MapClientActivity;
import com.idat.mototaxi.activities.client.RegisterActivity;
import com.idat.mototaxi.activities.driver.MapDriverActivity;
import com.idat.mototaxi.includes.MyToolbar;

import de.hdodenhof.circleimageview.CircleImageView;
import dmax.dialog.SpotsDialog;

public class LoginActivity extends AppCompatActivity {

    TextInputEditText txtEmail;
    TextInputEditText txtPassword;
    Button btnLogin;
    private CircleImageView btnBack;

    SharedPreferences preferences;

    FirebaseAuth auth;
    DatabaseReference database;

    AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //MyToolbar.show(this,"Login",true);

        preferences = getApplicationContext().getSharedPreferences("typeUser",MODE_PRIVATE);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();

        dialog = new SpotsDialog.Builder().setContext(LoginActivity.this).setMessage("Cargando...").build();

        txtEmail = findViewById(R.id.txtEmail);
        txtPassword = findViewById(R.id.txtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnBack = findViewById(R.id.circleImgBack);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    void login() {
        String email = txtEmail.getText().toString();
        String password = txtPassword.getText().toString();
        if(!email.isEmpty() && !password.isEmpty()){
            if(password.length()>=6){
                dialog.show();
                auth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            String typeUser = preferences.getString("user","");
                            if(typeUser.equals("client")){
                                Intent intent = new Intent(LoginActivity.this, MapClientActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);// para que el usuario ya no pueda ir hacia atras
                                startActivity(intent);
                            }else if(typeUser.equals("driver")){
                                Intent intent = new Intent(LoginActivity.this, MapDriverActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);// para que el usuario ya no pueda ir hacia atras
                                startActivity(intent);
                            }
                            //Toast.makeText(LoginActivity.this,"ingresaste al sistemas",Toast.LENGTH_SHORT).show();

                        }else{
                            Toast.makeText(LoginActivity.this,"La contraseña o el password son incorrectos",Toast.LENGTH_SHORT).show();
                        }
                        dialog.dismiss();
                    }
                });
            }else{
                Toast.makeText(LoginActivity.this,"La contraseña debe de tener minimo 6 caracteres",Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(LoginActivity.this,"Los campos de Usuario y Password no deben de estar vacios",Toast.LENGTH_SHORT).show();
        }
    }
}