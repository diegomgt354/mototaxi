package com.idat.mototaxi.providers;

import android.content.Context;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.idat.mototaxi.utils.CompressorBitmapImage;

import java.io.File;

public class ImageProvider {

    private StorageReference storageReference;

    public ImageProvider(String ref) {
        storageReference = FirebaseStorage.getInstance().getReference().child(ref);
    }

    public UploadTask saveImage(Context context, File image, String idUser){
        byte[] imageByte = CompressorBitmapImage.getImage(context, image.getPath(), 500, 500);
        final StorageReference storage = storageReference.child(idUser + ".jpg");
        storageReference = storage;
        UploadTask uploadTask = storage.putBytes(imageByte);
        return  uploadTask;
    }

    public StorageReference getStorage(){
        return  storageReference;
    }
}
