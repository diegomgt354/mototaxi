package com.idat.mototaxi.activities.client;

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
import com.idat.mototaxi.includes.MyToolbar;
import com.idat.mototaxi.models.Client;
import com.idat.mototaxi.providers.AuthProvider;
import com.idat.mototaxi.providers.ClientProvider;
import com.idat.mototaxi.providers.ImageProvider;
import com.idat.mototaxi.utils.CompressorBitmapImage;
import com.idat.mototaxi.utils.FileUtil;
import com.squareup.picasso.Picasso;

import java.io.File;

public class UpdateProfileActivity extends AppCompatActivity {


    private ImageView imageViewProfile;
    private Button btnUpdate;
    private TextView textViewNameToEdit;
    private ClientProvider clientProvider;
    private AuthProvider authProvider;
    private ImageProvider imageProvider;

    private File imgFile;
    private String urlImage;
    private String nameUser;

    private final int GALLERY_REQUEST = 1;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);
        MyToolbar.show(this, "Actualizar perfil", true);

        imageViewProfile = findViewById(R.id.imgViewProfile);
        btnUpdate = findViewById(R.id.btnUpdateProfile);
        textViewNameToEdit = findViewById(R.id.txtEditClientName);

        clientProvider = new ClientProvider();
        authProvider = new AuthProvider();
        imageProvider = new ImageProvider("client_images");

        progressDialog = new ProgressDialog(this);

        getClientInfo();

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

    private void getClientInfo(){
        clientProvider.getClient(authProvider.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String name = snapshot.child("name").getValue().toString();
                    String image = "";
                    if(snapshot.hasChild("image")){
                        image = snapshot.child("image").getValue().toString();
                        Picasso.with(UpdateProfileActivity.this).load(image).into(imageViewProfile);
                    }
                    textViewNameToEdit.setText(name);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void updateProfile() {
        nameUser = textViewNameToEdit.getText().toString();
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
        imageProvider.saveImage(UpdateProfileActivity.this, imgFile, authProvider.getId()).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if(task.isSuccessful()){
                    imageProvider.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String img = uri.toString();
                            Client client = new Client();
                            client.setImage(img);
                            client.setName(nameUser);
                            client.setId(authProvider.getId());
                            clientProvider.update(client).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    progressDialog.dismiss();
                                    Toast.makeText(UpdateProfileActivity.this, "La informacion se actualizo correctamente", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                }else{
                    Toast.makeText(UpdateProfileActivity.this, "Hubo un error al subir la imagen", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}