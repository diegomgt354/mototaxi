package com.idat.mototaxi.activities.driver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.idat.mototaxi.R;
import com.idat.mototaxi.activities.client.MapClientBookingActivity;
import com.idat.mototaxi.activities.client.UpdateProfileActivity;
import com.idat.mototaxi.includes.MyToolbar;
import com.idat.mototaxi.models.Client;
import com.idat.mototaxi.models.Driver;
import com.idat.mototaxi.providers.AuthProvider;
import com.idat.mototaxi.providers.ClientProvider;
import com.idat.mototaxi.providers.DriverProvider;
import com.idat.mototaxi.providers.ImageProvider;
import com.idat.mototaxi.utils.CompressorBitmapImage;
import com.idat.mototaxi.utils.FileUtil;
import com.squareup.picasso.Picasso;

import java.io.File;

public class UpdateProfileDriverActivity extends AppCompatActivity {
    private ImageView imageViewProfile;
    private Button btnUpdate;
    private TextView textViewNameToEdit;
    private TextView textViewBrandToEdit;
    private TextView textViewPlateToEdit;

    private DriverProvider driverProvider;
    private AuthProvider authProvider;
    private ImageProvider imageProvider;

    private File imgFile;
    private String urlImage;
    private String nameUser;
    private String vehicleBrand;
    private String vehiclePlate;

    private final int GALLERY_REQUEST = 1;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile_driver);
        MyToolbar.show(this, "Actualizar perfil", true);

        imageViewProfile = findViewById(R.id.imgViewProfileDriver);
        btnUpdate = findViewById(R.id.btnUpdateProfileDriver);
        textViewNameToEdit = findViewById(R.id.txtEditDriverName);
        textViewBrandToEdit = findViewById(R.id.txtDriverMarcaToEdit);
        textViewPlateToEdit = findViewById(R.id.txtDriverPlacaToEdit);


        authProvider = new AuthProvider();
        driverProvider = new DriverProvider();
        imageProvider = new ImageProvider("driver_images");

        progressDialog = new ProgressDialog(this);

        getDriverInfo();

        imageViewProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGalery();
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateProfile();
            }
        });

    }

    private void openGalery() {
        Intent galeryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galeryIntent.setType("image/*");
        startActivityForResult(galeryIntent, GALLERY_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == GALLERY_REQUEST && resultCode == RESULT_OK){
            try {
                imgFile = FileUtil.from(this, data.getData());
                imageViewProfile.setImageBitmap(BitmapFactory.decodeFile(imgFile.getAbsolutePath()));
            }catch (Exception e){
                Log.d("Error", "Mensaje: " + e.getMessage());
            }
        }
    }

    private void getDriverInfo(){
        driverProvider.getDriver(authProvider.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String name = snapshot.child("name").getValue().toString();
                    String brand = snapshot.child("marca").getValue().toString();
                    String plate = snapshot.child("placa").getValue().toString();
                    String image = "";
                    if(snapshot.hasChild("image")){
                        image = snapshot.child("image").getValue().toString();
                        Picasso.with(UpdateProfileDriverActivity.this).load(image).into(imageViewProfile);
                    }
                    textViewNameToEdit.setText(name);
                    textViewBrandToEdit.setText(brand);
                    textViewPlateToEdit.setText(plate);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void updateProfile() {
        nameUser = textViewNameToEdit.getText().toString();
        vehicleBrand = textViewBrandToEdit.getText().toString();
        vehiclePlate = textViewPlateToEdit.getText().toString();
        if(!nameUser.equals("") && imgFile != null){
            progressDialog.setMessage("Espera un momento...");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();

            saveImage();
        }else{
            Toast.makeText(this, "Ingresa una imagen y el nombre", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImage() {
        imageProvider.saveImage(UpdateProfileDriverActivity.this, imgFile, authProvider.getId()).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if(task.isSuccessful()){
                    imageProvider.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String img = uri.toString();
                            Driver driver = new Driver();
                            driver.setImage(img);
                            driver.setName(nameUser);
                            driver.setMarca(vehicleBrand);
                            driver.setPlaca(vehiclePlate);
                            driver.setId(authProvider.getId());
                            driverProvider.update(driver).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    progressDialog.dismiss();
                                    Toast.makeText(UpdateProfileDriverActivity.this, "La informacion se actualizo correctamente", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                }else{
                    Toast.makeText(UpdateProfileDriverActivity.this, "Hubo un error al subir la imagen", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
