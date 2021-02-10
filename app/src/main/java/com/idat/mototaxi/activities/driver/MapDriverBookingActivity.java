package com.idat.mototaxi.activities.driver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.idat.mototaxi.R;
import com.idat.mototaxi.models.FCMBody;
import com.idat.mototaxi.models.FCMResponse;
import com.idat.mototaxi.models.Info;
import com.idat.mototaxi.providers.AuthProvider;
import com.idat.mototaxi.providers.ClientBookingProvider;
import com.idat.mototaxi.providers.ClientProvider;
import com.idat.mototaxi.providers.GeofireProvider;
import com.idat.mototaxi.providers.GoogleAPIProvider;
import com.idat.mototaxi.providers.InfoProvider;
import com.idat.mototaxi.providers.NotificationProvider;
import com.idat.mototaxi.providers.TokenProvider;
import com.idat.mototaxi.utils.DecodePoints;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapDriverBookingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private SupportMapFragment mapFragment;
    private GeofireProvider geofireProvider;
    private ClientProvider clientProvider;
    private ClientBookingProvider clientBookingProvider;
    private NotificationProvider notificationProvider;
    private InfoProvider infoProvider;

    private AuthProvider authProvider;
    private TokenProvider tokenProvider;
    private Info info;

    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient; //ubicacvion en tiempos de ejecucion

    private final static int LOCATION_REQUEST_CODE = 1;//bandera para saber si deberia pedir permisos de ubicacion
    private final static int SETTING_REQUEST_CODE = 2;

    private Marker marker;
    private LatLng currentLatLng;

    private TextView textViewNameClientBooking;
    private TextView textViewEmailClientBooking;
    private TextView textViewOriginClientBooking;
    private TextView textViewDestinationClientBooking;
    private TextView textViewTime;
    private ImageView imageViewBooking;

    private String clientId;

    private LatLng originLatLng;
    private LatLng destinationLatLng;

    private GoogleAPIProvider googleAPIProvider;
    private List<LatLng> poligonosList;
    private PolylineOptions polylineOptions;

    private boolean firstTime = true;
    private boolean isCloseToClient = false;


    private Button btnStartBooking;
    private Button btnFinishBooking;

    double distanceInMeters = 1;
    int minutes = 0;
    int seconds = 0;
    boolean secondsIsOver = false;
    boolean rideStart = false;

    Handler handler = new Handler();
    Location previousLocation = new Location("");

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            seconds++;
            if(!secondsIsOver){
                textViewTime.setText(seconds + " seg");
            }else{
                textViewTime.setText(minutes + " min " + seconds + " seg");
            }
            if(seconds == 59){
                seconds = 0;
                secondsIsOver = true;
                minutes++;
            }
            handler.postDelayed(runnable, 1000);
        }
    };

    private LocationCallback locationCallback = new LocationCallback() {//callback que escucha cada ves que el usuario se ueva
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {

                    //Definimos la localizacion del usuario cada vez q este se mueva
                    currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    if(marker != null){
                        marker.remove();
                    }

                    if(rideStart){
                        distanceInMeters = distanceInMeters + previousLocation.distanceTo(location);
                    }

                    previousLocation = location;

                    marker = map.addMarker(new MarkerOptions().position(
                            new LatLng(location.getLatitude(), location.getLongitude())
                            ).title("Tu posicion")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.bicitaxi))
                    );

                    //se obtiene localizacion a tiempo real
                    map.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(location.getLatitude(), location.getLongitude()))
                                    .zoom(15f)
                                    .build()
                    ));

                    //Guardamos la localizacion
                    updateLocation();


                    if(firstTime){
                        firstTime = false;
                        //Obteniendo informacion del viaje
                        getClientBooking();
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_driver_booking);

        authProvider = new AuthProvider();
        geofireProvider = new GeofireProvider("drivers_working");
        tokenProvider = new TokenProvider();
        clientProvider = new ClientProvider();
        clientBookingProvider = new ClientBookingProvider();
        notificationProvider = new NotificationProvider();
        infoProvider = new InfoProvider();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);//con esta propiedad de inicia o detiene la ubicacion del usuario cuando querramos

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this); // dice que implementa el mapa aqui XD


        textViewNameClientBooking = findViewById(R.id.textViewNameClientBooking); //Nombre del cliente
        textViewEmailClientBooking = findViewById(R.id.textViewEmailClientBooking); //Email del cliente
        textViewOriginClientBooking = findViewById(R.id.textViewOriginClientBooking); //Origen del cliente
        textViewDestinationClientBooking = findViewById(R.id.textViewDestinationClientBooking); //Destino del cliente
        imageViewBooking = findViewById(R.id.imageViewClientBooking); //Imagen del cliente;
        textViewTime = findViewById(R.id.textViewTimeDriver);

        btnStartBooking = findViewById(R.id.btnStartBooking);
        btnFinishBooking = findViewById(R.id.btnFinishBooking);

        getInfo();
        //btnStartBooking.setEnabled(false);

        clientId = getIntent().getStringExtra("idClient");
        googleAPIProvider = new GoogleAPIProvider(MapDriverBookingActivity.this);

        //Obteniendo informacion del cliente
        getClient();

        btnStartBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isCloseToClient){
                    startBooking();
                }else{
                    Toast.makeText(MapDriverBookingActivity.this, "Debes acercarte mas a la posicion de recogida", Toast.LENGTH_SHORT).show();
                }

            }
        });

        btnFinishBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishBooking();
            }
        });

    }

    private void calculateRide(){
        if(minutes == 0){
            minutes = 1;
        }
        double priceMin = minutes * info.getMin();
        double priceKm = (distanceInMeters / 1000) * info.getKm();
        final double total = priceKm + priceMin;
        clientBookingProvider.updatePrice(clientId, total).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                clientBookingProvider.updateStatus(clientId, "finish");
                Intent intent = new Intent(MapDriverBookingActivity.this, CalificationClientActivity.class);
                intent.putExtra("idClient", clientId);
                intent.putExtra("price", total);
                startActivity(intent);
                finish();
            }
        });
    }


    private void getInfo() {
        infoProvider.getInfo().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    info = snapshot.getValue(Info.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void startBooking() {
        clientBookingProvider.updateStatus(clientId, "start");
        btnStartBooking.setVisibility(View.GONE);
        btnFinishBooking.setVisibility(View.VISIBLE);
        map.clear();
        map.addMarker(new MarkerOptions().position(destinationLatLng).title("Destino").icon(BitmapDescriptorFactory.fromResource(R.drawable.map_red)));
        drawRoute(destinationLatLng);
        sendNotification("Viaje iniciado");
        rideStart = true;
        handler.postDelayed(runnable, 1000);
    }

    private void finishBooking(){
        clientBookingProvider.updateIdHistoryBooking(clientId).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                sendNotification("Viaje finalizado");
                if(fusedLocationProviderClient != null){
                    fusedLocationProviderClient.removeLocationUpdates(locationCallback);
                }
                geofireProvider.removeLocation(authProvider.getId());
                if(handler != null){
                    handler.removeCallbacks(runnable);
                }
                calculateRide();
            }
        });
    }

    //Conociendo la distancia del cliente al conductor para recogerlo
    private double getDistanceBetween(LatLng clientLatLng, LatLng driverLatLng){
        double  distance = 0;
        Location clientLocation = new Location("");
        Location driverLocation = new Location("");
        clientLocation.setLatitude(clientLatLng.latitude);
        clientLocation.setLongitude(clientLatLng.longitude);
        driverLocation.setLatitude(driverLatLng.latitude);
        driverLocation.setLongitude(driverLatLng.longitude);

        distance = clientLocation.distanceTo(driverLocation);

        return distance;
    }

    private void getClientBooking() {
        clientBookingProvider.getClientBooking(clientId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String destination = snapshot.child("destination").getValue().toString();
                    String origin = snapshot.child("origin").getValue().toString();
                    double destinationLat = Double.parseDouble(snapshot.child("destinationLat").getValue().toString());
                    double destinationLng = Double.parseDouble(snapshot.child("destinationLng").getValue().toString());
                    double originLat = Double.parseDouble(snapshot.child("originLat").getValue().toString());
                    double originLng = Double.parseDouble(snapshot.child("originLng").getValue().toString());

                    originLatLng = new LatLng(originLat, originLng);
                    destinationLatLng = new LatLng(destinationLat, destinationLng);
                    textViewOriginClientBooking.setText("Recoger en: " + origin);
                    textViewDestinationClientBooking.setText("Destino: " + destination);
                    map.addMarker(new MarkerOptions().position(originLatLng).title("Recoger aqui").icon(BitmapDescriptorFactory.fromResource(R.drawable.map_blue)));
                    drawRoute(originLatLng);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getClient() {
        clientProvider.getClient(clientId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String email = snapshot.child("email").getValue().toString();
                    String name = snapshot.child("name").getValue().toString();
                    String image = "";
                    if(snapshot.hasChild("image")){
                        image = snapshot.child("image").getValue().toString();
                        Picasso.with(MapDriverBookingActivity.this).load(image).into(imageViewBooking);
                    }
                    textViewNameClientBooking.setText("Nombre del cliente: " + name);
                    textViewEmailClientBooking.setText("Email del cliente: " + email);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void drawRoute(LatLng latLng){
        googleAPIProvider.getDirections(currentLatLng, latLng).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                try{
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


                }catch (Exception e){
                    Log.d("Error", "Ups! Al parecer no funciono: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {

            }
        });
    }

    private void updateLocation(){
        if(authProvider.existSession() && currentLatLng != null){
            geofireProvider.saveLocation(authProvider.getId(), currentLatLng);
            if(!isCloseToClient){
                if(originLatLng != null && currentLatLng != null){
                    double distance = getDistanceBetween(originLatLng, currentLatLng); // DISTANCIA EN METROS
                    if(distance <= 200){
                        //btnStartBooking.setEnabled(true);
                        isCloseToClient = true;
                        Toast.makeText(this, "Estas cerca de la posicion de recogida", Toast.LENGTH_SHORT).show();
                    }
                }

            }

        }

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.getUiSettings().setZoomControlsEnabled(true);

        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);//para que haga uso del gps copn la mayor presicion posible
        locationRequest.setSmallestDisplacement(5);

        startLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {//si el usuario concedio los permisos
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {//usuario concedio los permisos de ubicacion
                    if(gpsActive()){
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());//activar escuchador de nuestra ubicacion actual
                        map.setMyLocationEnabled(true);
                    }else{
                        showAlertDialogNOGPS();
                    }
                } else {
                    checkLocationPermissions();
                }
            } else {
                checkLocationPermissions();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTING_REQUEST_CODE && gpsActive()) {
            //codigo nuevo en la nueva version de androis alt+enter
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());//activar escuchador de nuestra ubicacion actual
            map.setMyLocationEnabled(true);
        }else{
            showAlertDialogNOGPS();
        }

    }

    private void showAlertDialogNOGPS(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Por favor activa tu ubicación para continuar")
                .setPositiveButton("Configuraciones", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),SETTING_REQUEST_CODE);
                    }
                }).create().show();
    }

    private boolean gpsActive(){
        boolean isActive = false;
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            isActive = true;
        }
        return isActive;
    }

    //Metodo para desconectarnos del GPS
    private void disconnect(){
        if(fusedLocationProviderClient != null){
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            if(authProvider.existSession()){
                geofireProvider.removeLocation(authProvider.getId());
            }
        }else{
            Toast.makeText(this, "No te puedes desconectar", Toast.LENGTH_SHORT).show();
        }
    }


    //metodo para iniciar escuchador de nuestra ubicacion
    private void startLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {//usuario concedio los permisos de ubicacion
                if(gpsActive()){
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());//activar escuchador de nuestra ubicacion actual
                    map.setMyLocationEnabled(true);
                }else{
                    showAlertDialogNOGPS();
                }
            }else{
                checkLocationPermissions();
            }
        }else{
            if(gpsActive()){
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());//activar escuchador de nuestra ubicacion actual
                map.setMyLocationEnabled(true);
            }else{
                showAlertDialogNOGPS();
            }
        }
    }

    private void checkLocationPermissions(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){//usuario no concedio permisos
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                new AlertDialog.Builder(this)
                        .setTitle("Proporciona los permisos para continuar")
                        .setMessage("Esta aplicacion require los permisos de ubicacion para poder utilizarse")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MapDriverBookingActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);//habilita permisos
                            }
                        })
                        .create()
                        .show();
            }else{
                ActivityCompat.requestPermissions(MapDriverBookingActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);//habilita permisos
            }
        }

    }

    private void sendNotification(final String status) {
        tokenProvider.getToken(clientId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String token = snapshot.child("token").getValue().toString();
                    Map<String , String> map = new HashMap<>();
                    map.put("title", "ESTADO DE TU VIAJE");
                    map.put("body",
                            "Tu estado del viaje es: " + status
                    );
                    FCMBody fcmBody = new FCMBody(token, "high", "4500s" ,map);
                    notificationProvider.sendNotification(fcmBody).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                            if(response.body() != null){
                                if(response.body().getSuccess() != 1){
                                    Toast.makeText(MapDriverBookingActivity.this, "Ups! Al parecer no se puedo enviar la notificación", Toast.LENGTH_SHORT).show();

                                }
                            }else{
                                Toast.makeText(MapDriverBookingActivity.this, "Ups! Al parecer no se puedo enviar la notificación", Toast.LENGTH_SHORT).show();
                            }

                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call, Throwable t) {

                        }
                    });
                }else{
                    Toast.makeText(MapDriverBookingActivity.this, "Ups! Al parecer no se puedo enviar la notificación porque el conductor no tienen un token de sesion", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }
}

