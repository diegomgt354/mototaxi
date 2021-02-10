package com.idat.mototaxi.retrofit;

import com.idat.mototaxi.models.FCMBody;
import com.idat.mototaxi.models.FCMResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMApi {

    @Headers({
        "Content-Type:application/json",
            "Authorization:key=AAAAIBDozA0:APA91bFwvhOmA1GkyR7a3JKWP1v_1CLaEJDuGOy2QNUK6FFuhQ_ixiKhn_mHUk2w_YGgcKHq5ANt40LqMy12RKM7nP6KrLVZsb25eRd9ietBZbp3tLB4dcgegi5-P6YXycFjkaQk586t"
    })

    @POST("fcm/send")
    Call<FCMResponse> send(@Body FCMBody body);
}
