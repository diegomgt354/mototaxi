package com.idat.mototaxi.activities.driver;

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
import com.idat.mototaxi.R;
import com.idat.mototaxi.activities.client.RegisterActivity;
import com.idat.mototaxi.includes.MyToolbar;
import com.idat.mototaxi.models.Client;
import com.idat.mototaxi.models.Driver;
import com.idat.mototaxi.providers.AuthProvider;
import com.idat.mototaxi.providers.ClientProvider;
import com.idat.mototaxi.providers.DriverProvider;

import dmax.dialog.SpotsDialog;

public class RegisterDriverActivity extends AppCompatActivity {

    AuthProvider authProvider;
    DriverProvider driverProvider;

    TextInputEditText nameTxt;
    TextInputEditText emailTxt;
    TextInputEditText marcaVehicleTxt;
    TextInputEditText placaVehicleTxt;
    TextInputEditText passwordTxt;
    Button btnRegisterDriver;

    AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_driver);

        MyToolbar.show(this,"Registrar Conductor",true);

        authProvider = new AuthProvider();
        driverProvider = new DriverProvider();

        dialog = new SpotsDialog.Builder().setContext(RegisterDriverActivity.this).setMessage("Cargando...").build();

        nameTxt = findViewById(R.id.txtDriverNombre);
        emailTxt = findViewById(R.id.txtDriverCorreo);
        marcaVehicleTxt = findViewById(R.id.txtDriverMarca);
        placaVehicleTxt = findViewById(R.id.txtDriverPlaca);
        passwordTxt = findViewById(R.id.txtDriverPassword);

        btnRegisterDriver = findViewById(R.id.btnDriverRegister);

        btnRegisterDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickRegister();
            }
        });

    }

    void clickRegister() {
        String name = nameTxt.getText().toString();
        String email = emailTxt.getText().toString();
        String marcaVehicle = marcaVehicleTxt.getText().toString();
        String placaVehicle = placaVehicleTxt.getText().toString();
        String pass = passwordTxt.getText().toString();

        if(!name.isEmpty() && !email.isEmpty() && !pass.isEmpty() && !marcaVehicle.isEmpty() && !placaVehicle.isEmpty()){
            if(pass.length()>=6){
                register(name,email,marcaVehicle,placaVehicle,pass);
            }else{
                Toast.makeText(RegisterDriverActivity.this,"La contraseña debe de tener minimo 6 caracteres",Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(RegisterDriverActivity.this,"Los campos de Usuario y Password no deben d estar vacios",Toast.LENGTH_SHORT).show();
        }

    }

    void register(String name,String email, String marcaVehicle, String placaVehicle, String pass) {
        //dialog.show();
        authProvider.register(email,pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                dialog.hide();
                if(task.isSuccessful()){
                    String id = FirebaseAuth.getInstance().getCurrentUser().getUid();//obtiene el identificador de usuarios que se avaba de registrar
                    Driver driver = new Driver(id,name,email,marcaVehicle,placaVehicle);
                    create(driver);
                    //saveUser(id,name,email);
                }else{
                    Toast.makeText(RegisterDriverActivity.this,"No se pudo generar el Registro",Toast.LENGTH_SHORT).show();
                }
                //dialog.dismiss();
            }
        });
    }

    void create(Driver driver){
        driverProvider.create(driver).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(RegisterDriverActivity.this,"El registro se genero de manera existosa",Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterDriverActivity.this,MapDriverActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);// para que el usuario ya no pueda ir hacia atras
                    startActivity(intent);
                }else{
                    Toast.makeText(RegisterDriverActivity.this,"No se pudo generar el Registro",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}