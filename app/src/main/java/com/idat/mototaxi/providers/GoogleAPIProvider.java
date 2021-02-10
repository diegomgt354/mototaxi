package com.idat.mototaxi.providers;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;
import com.idat.mototaxi.R;
import com.idat.mototaxi.retrofit.IGoogleAPI;
import com.idat.mototaxi.retrofit.RetrofitClient;

import retrofit2.Call;

public class GoogleAPIProvider {

    private Context context;

    public GoogleAPIProvider(Context context){
        this.context = context;
    }

    public Call<String> getDirections(LatLng originLatLng, LatLng destinationLatLng){
        String baseUrl = "https://maps.googleapis.com";
        String query = "/maps/api/directions/json?mode=driving&transit_routing_preferences=less_driving&"
                + "origin=" + originLatLng.latitude + "," + originLatLng.longitude + "&"
                + "destination=" + destinationLatLng.latitude + "," + destinationLatLng.longitude + "&"
                + "key=" + context.getResources().getString(R.string.google_maps_key);

        return RetrofitClient.getClient(baseUrl).create(IGoogleAPI.class).getDirections(baseUrl + query);
    }
}
