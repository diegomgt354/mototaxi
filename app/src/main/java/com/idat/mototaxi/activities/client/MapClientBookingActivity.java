package com.idat.mototaxi.activities.client;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.idat.mototaxi.R;
import com.idat.mototaxi.activities.driver.CalificationClientActivity;
import com.idat.mototaxi.activities.driver.MapDriverBookingActivity;
import com.idat.mototaxi.providers.AuthProvider;
import com.idat.mototaxi.providers.ClientBookingProvider;
import com.idat.mototaxi.providers.DriverProvider;
import com.idat.mototaxi.providers.GeofireProvider;
import com.idat.mototaxi.providers.GoogleAPIProvider;
import com.idat.mototaxi.providers.TokenProvider;
import com.idat.mototaxi.utils.DecodePoints;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapClientBookingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private SupportMapFragment mapFragment;
    private AuthProvider authProvider;

    private GeofireProvider geofireProvider;
    private TokenProvider tokenProvider;
    private ClientBookingProvider clientBookingProvider;
    private DriverProvider driverProvider;

    private Marker markerDriver;

    private boolean firstTime = true;


    private String origin;
    private LatLng originLatLng;

    private String destination;
    private LatLng destinationLatLng;
    private LatLng driverLatLng;

    private TextView textViewNameDriverBooking;
    private TextView textViewEmailDriverBooking;
    private TextView textViewOriginDriverBooking;
    private TextView textViewDestinationDriverBooking;
    private TextView textViewStatusBooking;
    private ImageView imageViewBooking;

    private GoogleAPIProvider googleAPIProvider;
    private List<LatLng> poligonosList;
    private PolylineOptions polylineOptions;

    private ValueEventListener listener;
    private String mIdDriver;
    private ValueEventListener listenerStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_client_booking);

        authProvider = new AuthProvider();
        geofireProvider = new GeofireProvider("drivers_working");
        tokenProvider = new TokenProvider();
        clientBookingProvider = new ClientBookingProvider();
        googleAPIProvider = new GoogleAPIProvider(MapClientBookingActivity.this);
        driverProvider = new DriverProvider();

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this); // dice que implementa el mapa aqui XD

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));
        }

        textViewNameDriverBooking = findViewById(R.id.textViewNameDriverBooking); //Nombre del conductor
        textViewEmailDriverBooking = findViewById(R.id.textViewEmailDriverBooking); //Email del conductor
        textViewOriginDriverBooking = findViewById(R.id.textViewOriginDriverBooking); //Origen del conductor
        textViewDestinationDriverBooking = findViewById(R.id.textViewDestinationDriverBooking); //Destino del conductor
        textViewStatusBooking = findViewById(R.id.textViewStatusBooking);
        imageViewBooking = findViewById(R.id.imageViewDriverBooking);

        getStatus();

        getClientBooking();
    }

    private void getStatus() {
        listenerStatus = clientBookingProvider.getStatus(authProvider.getId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.getValue().toString();
                    if (status.equals("accept")) {
                        textViewStatusBooking.setText("Estado: Aceptado");
                    }
                    if (status.equals("start")) {
                        textViewStatusBooking.setText("Estado: Viaje Iniciado");
                        startBooking();
                    } else if (status.equals("finish")) {
                        textViewStatusBooking.setText("Estado: Viaje finalizado");
                        finishBooking();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void finishBooking() {
        Intent intent = new Intent(MapClientBookingActivity.this, CalificationDriverActivity.class);
        startActivity(intent);
        finish();
    }

    private void startBooking() {
        map.clear();
        map.addMarker(new MarkerOptions().position(destinationLatLng).title("Destino").icon(BitmapDescriptorFactory.fromResource(R.drawable.map_red)));
        drawRoute(destinationLatLng);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) {
            geofireProvider.getDriverLocation(mIdDriver).removeEventListener(listener);
        }
        if (listenerStatus != null) {
            clientBookingProvider.getStatus(authProvider.getId()).removeEventListener(listenerStatus);
        }
    }


    private void getClientBooking() {
        clientBookingProvider.getClientBooking(authProvider.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String destination = snapshot.child("destination").getValue().toString();
                    String origin = snapshot.child("origin").getValue().toString();
                    String idDriver = snapshot.child("idDriver").getValue().toString();
                    mIdDriver = idDriver;
                    double destinationLat = Double.parseDouble(snapshot.child("destinationLat").getValue().toString());
                    double destinationLng = Double.parseDouble(snapshot.child("destinationLng").getValue().toString());
                    double originLat = Double.parseDouble(snapshot.child("originLat").getValue().toString());
                    double originLng = Double.parseDouble(snapshot.child("originLng").getValue().toString());

                    originLatLng = new LatLng(originLat, originLng);
                    destinationLatLng = new LatLng(destinationLat, destinationLng);
                    textViewOriginDriverBooking.setText("Recoger en: " + origin);
                    textViewDestinationDriverBooking.setText("Destino: " + destination);
                    map.addMarker(new MarkerOptions().position(originLatLng).title("Recoger aqui").icon(BitmapDescriptorFactory.fromResource(R.drawable.map_blue)));

                    getDriver(idDriver);
                    //Obtenemos la localizacion del conductor
                    getDriverLocation(idDriver);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getDriver(String idDriver) {
        driverProvider.getDriver(idDriver).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue().toString();
                    String email = snapshot.child("email").getValue().toString();
                    String image = "";
                    if(snapshot.hasChild("image")){
                        image = snapshot.child("image").getValue().toString();
                        Picasso.with(MapClientBookingActivity.this).load(image).into(imageViewBooking);
                    }
                    textViewNameDriverBooking.setText("Nombre del conductor: " + name);
                    textViewEmailDriverBooking.setText("Email del conductor: " + email);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void  getDriverLocation(String idDriver) {
        listener = geofireProvider.getDriverLocation(idDriver).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    double lat = Double.parseDouble(snapshot.child("0").getValue().toString());
                    double lng = Double.parseDouble(snapshot.child("1").getValue().toString());
                    driverLatLng = new LatLng(lat, lng);
                    if (markerDriver != null) {
                        markerDriver.remove();
                    }
                    markerDriver = map.addMarker(new MarkerOptions()
                            .position(new LatLng(lat, lng))
                            .title("Tu conductor")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.bicitaxi))
                    );

                    if (firstTime) {
                        firstTime = false;
                        map.animateCamera(CameraUpdateFactory.newCameraPosition(
                                new CameraPosition.Builder()
                                        .target(driverLatLng)
                                        .zoom(14f)
                                        .build()
                        ));
                        drawRoute(originLatLng);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void drawRoute(LatLng latLng) {
        googleAPIProvider.getDirections(driverLatLng, latLng).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                try {
                    JSONObject jsonObject = new JSONObject(response.body());
                    JSONArray jsonArray = jsonObject.getJSONArray("routes");
                    JSONObject route = jsonArray.getJSONObject(0);
                    JSONObject poligonos = route.getJSONObject("overview_polyline");
                    String points = poligonos.getString("points");
                    poligonosList = DecodePoints.decodePoly(points);
                    polylineOptions = new PolylineOptions();
                    polylineOptions.color(Color.DKGRAY);
                    polylineOptions.width(13f);
                    polylineOptions.startCap(new SquareCap());
                    polylineOptions.jointType(JointType.ROUND);
                    polylineOptions.addAll(poligonosList);
                    map.addPolyline(polylineOptions);

                    //Obteniendo la disctancia y el tiempo a traves del api
                    JSONArray legs = route.getJSONArray("legs");
                    JSONObject leg = legs.getJSONObject(0);
                    JSONObject distance = leg.getJSONObject("distance");
                    JSONObject duration = leg.getJSONObject("duration");

                    //Almancenando esa distancia y ese tiempo
                    String distanceText = distance.getString("text");
                    String durationText = duration.getString("text");


                } catch (Exception e) {
                    Log.d("Error", "Ups! Al parecer no funciono: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.getUiSettings().setZoomControlsEnabled(true);


    }

}