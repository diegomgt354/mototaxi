package com.idat.mototaxi.activities.client;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.idat.mototaxi.R;
import com.idat.mototaxi.models.ClientBooking;
import com.idat.mototaxi.models.FCMBody;
import com.idat.mototaxi.models.FCMResponse;
import com.idat.mototaxi.providers.AuthProvider;
import com.idat.mototaxi.providers.ClientBookingProvider;
import com.idat.mototaxi.providers.GeofireProvider;
import com.idat.mototaxi.providers.GoogleAPIProvider;
import com.idat.mototaxi.providers.NotificationProvider;
import com.idat.mototaxi.providers.TokenProvider;
import com.idat.mototaxi.utils.DecodePoints;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RequestDriverActivity extends AppCompatActivity {


    private LottieAnimationView animation;
    private TextView textViewBuscando;
    private Button btnCancelarViaje;
    private GeofireProvider geofireProvider;

    private String extraOrigin;
    private String extraDestination;
    private double extraOriginLat;
    private double extraOriginLng;
    private double extraDestinationLat;
    private double extraDestinationLng;
    private LatLng originLatLng;
    private LatLng destinationLatLng;


    private double radius = 0.1;
    private boolean driverFound = false;
    private String idDriverFound = "";
    private LatLng driverFoundLatLng;


    private NotificationProvider notificationProvider;
    private TokenProvider tokenProvider;
    private ClientBookingProvider clientBookingProvider;
    private AuthProvider authProvider;
    private GoogleAPIProvider googleAPIProvider;

    private ValueEventListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_driver);

        animation = findViewById(R.id.animation);
        textViewBuscando = findViewById(R.id.textViewBuscando);
        btnCancelarViaje = findViewById(R.id.btnCancelarViaje);

        animation.playAnimation();

        //Datos del viaje del cliente
        extraOrigin = getIntent().getStringExtra("origin");
        extraDestination = getIntent().getStringExtra("destination");

        //Obteniendo lat y lng de cada uno
        extraOriginLat = getIntent().getDoubleExtra("origin_lat", 0);
        extraOriginLng = getIntent().getDoubleExtra("origin_lng", 0);
        extraDestinationLat = getIntent().getDoubleExtra("destination_lat", 0);
        extraDestinationLng = getIntent().getDoubleExtra("destination_lng", 0);

        //Colocando la lat y lng en una variable
        originLatLng = new LatLng(extraOriginLat, extraOriginLng);
        destinationLatLng = new LatLng(extraDestinationLat, extraDestinationLng);

        geofireProvider = new GeofireProvider("active_drivers");
        notificationProvider = new NotificationProvider();
        tokenProvider = new TokenProvider();
        clientBookingProvider = new ClientBookingProvider();
        authProvider = new AuthProvider();
        googleAPIProvider = new GoogleAPIProvider(RequestDriverActivity.this);

        btnCancelarViaje.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelRequest();
            }
        });

        getClosesDrivers();
    }

    private void cancelRequest() {
        clientBookingProvider.delete(authProvider.getId()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                sendNotificationCancel();
            }
        });
    }

    //Obtener conductores disponibles para el cliente
    private void getClosesDrivers(){
        geofireProvider.getActiveDrivers(originLatLng, radius).addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound){
                    driverFound = true;
                    idDriverFound = key;
                    driverFoundLatLng = new LatLng(location.latitude, location.longitude);
                    textViewBuscando.setText("CONDUCTOR ENCONTRADO!\nEsperando respuesta..");
                    createClientBooking();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                //Se ejecutara cuando termina la busqueda de un conductor en el radio establecido ( 0.1 km)
                if(!driverFound){
                    radius += 0.1f; //Si no lo encuentra aumentaremos el radio de busqueda
                    //No encontro un conductor en un radio de 5km  y cancelara la busqueda
                    if(radius > 5){
                        textViewBuscando.setText("Ups! Al parecer no se encontro un conductor");
                        return;
                    }else{
                        getClosesDrivers();
                    }
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    //Creando informacion de la solicitud de viaje
    private void createClientBooking(){

        //Obteniendo el tiempo y la distancia
        googleAPIProvider.getDirections(originLatLng, driverFoundLatLng).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                try{
                    JSONObject jsonObject = new JSONObject(response.body());
                    JSONArray jsonArray = jsonObject.getJSONArray("routes");
                    JSONObject route = jsonArray.getJSONObject(0);
                    JSONObject poligonos = route.getJSONObject("overview_polyline");
                    String points = poligonos.getString("points");

                    //Obteniendo la disctancia y el tiempo a traves del api
                    JSONArray legs = route.getJSONArray("legs");
                    JSONObject leg = legs.getJSONObject(0);
                    JSONObject distance = leg.getJSONObject("distance");
                    JSONObject duration = leg.getJSONObject("duration");

                    //Almancenando esa distancia y ese tiempo
                    String distanceText = distance.getString("text");
                    String durationText = duration.getString("text");

                    //Enviando la notificacion
                    sendNotification(durationText, distanceText);

                }catch (Exception e){
                    Log.d("Error", "Ups! Al parecer no funciono: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {

            }
        });

    }

    private void sendNotificationCancel() {

        if (idDriverFound != null) {
            tokenProvider.getToken(idDriverFound).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {

                        if (dataSnapshot.hasChild("token")) {
                            String token = dataSnapshot.child("token").getValue().toString();
                            Map<String, String> map = new HashMap<>();
                            map.put("title", "VIAJE CANCELADO");
                            map.put("body",
                                    "El cliente cancelo la solicitud"
                            );
                            FCMBody fcmBody = new FCMBody(token, "high", "4500s", map);
                            notificationProvider.sendNotification(fcmBody).enqueue(new Callback<FCMResponse>() {
                                @Override
                                public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                                    if (response.body() != null) {
                                        if (response.body().getSuccess() == 1) {
                                            Toast.makeText(RequestDriverActivity.this, "La solicitud se cancelo correctamente", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(RequestDriverActivity.this, MapClientActivity.class);
                                            startActivity(intent);
                                            finish();
                                            //Toast.makeText(RequestDriverActivity.this, "La notificacion se ha enviado correctamente", Toast.LENGTH_SHORT).show();
                                        }
                                        else {
                                            Toast.makeText(RequestDriverActivity.this, "No se pudo enviar la notificacion", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    else {
                                        Toast.makeText(RequestDriverActivity.this, "No se pudo enviar la notificacion", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<FCMResponse> call, Throwable t) {
                                    Log.d("Error", "Error " + t.getMessage());
                                }
                            });
                        }
                        else {
                            Toast.makeText(RequestDriverActivity.this, "La solicitud se cancelo correctamente", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RequestDriverActivity.this, MapClientActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }
                    else {
                        Toast.makeText(RequestDriverActivity.this, "No se pudo enviar la notificacion porque el conductor no tiene un token de sesion", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        else {
            Toast.makeText(RequestDriverActivity.this, "La solicitud se cancelo correctamente", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(RequestDriverActivity.this, MapClientActivity.class);
            startActivity(intent);
            finish();
        }

    }

    //Enviar las noticaciones de servicio a los conductores
    private void sendNotification(final String time, final String distanceKm) {
        tokenProvider.getToken(idDriverFound).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String token = snapshot.child("token").getValue().toString();
                    Map<String , String> map = new HashMap<>();
                    map.put("title", "SOLICITUD DE SERVICIO A " + time + " DE TU POSICIÓN");
                    map.put("body",
                            "Un cliente esta solicitando un servicio a una distancia de " + distanceKm +
                                    "\nRecoger en: " + extraOrigin +
                                    "\nDestino: " + extraDestination
                    );
                    map.put("idClient", authProvider.getId());
                    map.put("origin", extraOrigin);
                    map.put("destination", extraDestination);
                    map.put("min", time);
                    map.put("distance", distanceKm);
                    FCMBody fcmBody = new FCMBody(token, "high", "4500s" , map);
                    notificationProvider.sendNotification(fcmBody).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                            if(response.body() != null){
                                if(response.body().getSuccess() == 1){

                                    //Enviando los valores para la notificacion
                                    ClientBooking clientBooking = new ClientBooking(
                                            authProvider.getId(),
                                            idDriverFound,
                                            extraDestination,
                                            extraOrigin,
                                            time,
                                            distanceKm,
                                            "create",
                                            extraOriginLat,
                                            extraOriginLng,
                                            extraDestinationLat,
                                            extraDestinationLng
                                    );
                                    clientBookingProvider.create(clientBooking).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            checkStatusClientBooking();
                                        }


                                    });

                                    //Toast.makeText(RequestDriverActivity.this, "La notificacion se ha enviado correctamente", Toast.LENGTH_SHORT).show();
                                }else{
                                    Toast.makeText(RequestDriverActivity.this, "Ups! Al parecer no se puedo enviar la notificación", Toast.LENGTH_SHORT).show();
                                }
                            }else{
                                Toast.makeText(RequestDriverActivity.this, "Ups! Al parecer no se puedo enviar la notificación", Toast.LENGTH_SHORT).show();
                            }

                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call, Throwable t) {

                        }
                    });
                }else{
                    Toast.makeText(RequestDriverActivity.this, "Ups! Al parecer no se puedo enviar la notificación porque el conductor no tienen un token de sesion", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    //Checando el valor de status para poder ver si acepto o cancelo el viaje
    private void checkStatusClientBooking() {
        //Lo colocamos en una variable para poder almacernarlo y asi detenerlo porque esta funcion seguira escuchando
        //a la base de datos constatmente
        listener = clientBookingProvider.getStatus(authProvider.getId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String status = snapshot.getValue().toString();
                    if(status.equals("accept")){
                        Intent intent = new Intent(RequestDriverActivity.this, MapClientBookingActivity.class);
                        startActivity(intent);
                        finish();
                    }else if(status.equals("cancel")){
                        Toast.makeText(RequestDriverActivity.this, "El conductor cancelo el viaje", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RequestDriverActivity.this, MapClientActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //Aqui detenemos el listener
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(listener != null){
            clientBookingProvider.getStatus(authProvider.getId()).removeEventListener(listener);
        }
    }
}