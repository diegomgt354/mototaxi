package com.idat.mototaxi.providers;



import com.idat.mototaxi.models.FCMBody;
import com.idat.mototaxi.models.FCMResponse;
import com.idat.mototaxi.retrofit.IFCMApi;
import com.idat.mototaxi.retrofit.RetrofitClient;

import retrofit2.Call;

public class NotificationProvider {

    private String url = "https://fcm.googleapis.com";

    public NotificationProvider() {
    }

    public Call<FCMResponse> sendNotification(FCMBody body){
        return RetrofitClient.getClientObject(url).create(IFCMApi.class).send(body);
    }
}
