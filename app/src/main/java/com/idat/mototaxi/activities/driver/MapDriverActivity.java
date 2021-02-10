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
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.idat.mototaxi.R;
import com.idat.mototaxi.activities.MainActivity;
import com.idat.mototaxi.activities.client.HistoryBookingClientActivity;
import com.idat.mototaxi.activities.client.MapClientActivity;
import com.idat.mototaxi.activities.client.UpdateProfileActivity;
import com.idat.mototaxi.includes.MyToolbar;
import com.idat.mototaxi.providers.AuthProvider;
import com.idat.mototaxi.providers.GeofireProvider;
import com.idat.mototaxi.providers.TokenProvider;

public class MapDriverActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private SupportMapFragment mapFragment;
    private GeofireProvider geofireProvider;

    private AuthProvider authProvider;
    private TokenProvider tokenProvider;

    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient; //ubicacvion en tiempos de ejecucion

    private final static int LOCATION_REQUEST_CODE = 1;//bandera para saber si deberia pedir permisos de ubicacion

    private final static int SETTING_REQUEST_CODE = 2;

    private Marker marker;
    private Button buttonConectar;
    private boolean isConnect = false;

    private LatLng currentLatLng;

    private ValueEventListener listener;

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

                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_driver);

        MyToolbar.show(this, "Conductor", false);

        authProvider = new AuthProvider();
        geofireProvider = new GeofireProvider("active_drivers");
        tokenProvider = new TokenProvider();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);//con esta propiedad de inicia o detiene la ubicacion del usuario cuando querramos

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this); // dice que implementa el mapa aqui XD

        buttonConectar = findViewById(R.id.btnConectar);
        buttonConectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isConnect){
                    disconnect();
                }else{
                    startLocation();
                }
            }
        });

        generateToken();
        isDriverWorking();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(locationCallback != null && fusedLocationProviderClient != null){
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
        if(listener != null){
            geofireProvider.isDriverWorking(authProvider.getId()).removeEventListener(listener);
        }
    }

    //Preguntamos si existe el nodo de "drivers_working" (CONDUCTORES TRABAJANDO)
    private void isDriverWorking() {
        listener = geofireProvider.isDriverWorking(authProvider.getId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    disconnect();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void updateLocation(){
        if(authProvider.existSession() && currentLatLng != null){
            geofireProvider.saveLocation(authProvider.getId(), currentLatLng);
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
            buttonConectar.setText("CONECTARSE");
            isConnect = false;
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
                    buttonConectar.setText("DESCONECTARSE");
                    isConnect = true;
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
                        ActivityCompat.requestPermissions(MapDriverActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);//habilita permisos
                    }
                })
                        .create()
                        .show();
            }else{
                ActivityCompat.requestPermissions(MapDriverActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);//habilita permisos
            }
        }

        }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.driver_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==R.id.action_logout){
            logout();
        }
        if(item.getItemId() == R.id.action_update){
            Intent intent = new Intent(MapDriverActivity.this, UpdateProfileDriverActivity.class);
            startActivity(intent);
        }
        if(item.getItemId() == R.id.action_history){
            Intent intent = new Intent(MapDriverActivity.this, HistoryBookingDriverActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    void logout(){
        disconnect();
        authProvider.logout();
        Intent intent = new Intent(MapDriverActivity.this, MainActivity.class);
        startActivity(intent);
        finish();//finaliza la actividad
    }

    //Generaremos un token para el mensaje de notificacion
    void generateToken(){
        tokenProvider.create(authProvider.getId());
    }
}


