package com.idat.mototaxi.activities.client;

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
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
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
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.database.DatabaseError;
import com.google.maps.android.SphericalUtil;
import com.idat.mototaxi.R;
import com.idat.mototaxi.activities.MainActivity;
import com.idat.mototaxi.activities.driver.MapDriverActivity;
import com.idat.mototaxi.includes.MyToolbar;
import com.idat.mototaxi.providers.AuthProvider;
import com.idat.mototaxi.providers.GeofireProvider;
import com.idat.mototaxi.providers.TokenProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapClientActivity extends AppCompatActivity implements OnMapReadyCallback {


    private GoogleMap map;
    private SupportMapFragment mapFragment;
    private AuthProvider authProvider;

    private GeofireProvider geofireProvider;
    private TokenProvider tokenProvider;

    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient; //ubicacvion en tiempos de ejecucion

    private final static int LOCATION_REQUEST_CODE = 1;//bandera para saber si deberia pedir permisos de ubicacion

    private final static int SETTINGS_REQUEST_CODE = 2;

    private Marker marker;
    private LatLng currentLatLng;

    private List<Marker> driversMarkers = new ArrayList<>();

    private boolean firstTime = true;

    private AutocompleteSupportFragment autocomplete;
    private AutocompleteSupportFragment autocompleteDestination;
    private PlacesClient places;

    private String origin;
    private LatLng originLatLng;

    private String destination;
    private LatLng destinationLatLng;

    private GoogleMap.OnCameraIdleListener cameraIdleListener;

    private Button btnrequestDriver;

    private LocationCallback locationCallback = new LocationCallback() {//callback que escucha cada ves que el usuario se ueva
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {
                    //Definimos la localizacion del usuario cada vez q este se mueva
                    currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    /*if(marker != null){
                        marker.remove();
                    }

                    marker = map.addMarker(new MarkerOptions().position(
                            new LatLng(location.getLatitude(), location.getLongitude())
                            ).title("Tu posicion")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ubicacionusuario))
                    );*/

                    //se obtiene localizacion a tiempo real
                    map.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(location.getLatitude(), location.getLongitude()))
                                    .zoom(15f)
                                    .build()
                    ));

                    if(firstTime){
                        firstTime = false;
                        getActivateDrivers();
                        limitSearch();
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_client);
        MyToolbar.show(this,"Cliente",false);

        authProvider = new AuthProvider();
        geofireProvider = new GeofireProvider("active_drivers");
        tokenProvider = new TokenProvider();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);//con esta propiedad de inicia o detiene la ubicacion del usuario cuando querramos


        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this); // dice que implementa el mapa aqui XD
        btnrequestDriver = findViewById(R.id.btnRequestDriver);

        if(!Places.isInitialized()){
            Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));
        }

        places = Places.createClient(this);
        //Obteniendo el lugar de origen
        instanceAutocompleteOrigin();
        //Obteniendo el lugar de destino
        instanceAutocompleteDestination();

        onCameraMove();

        btnrequestDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestDriver();
            }
        });

        generateToken();
    }

    //Solicitar un conductor
    private void requestDriver(){
        if(originLatLng != null && destinationLatLng != null){
            Intent intent = new Intent(MapClientActivity.this, DetailRequestActivity.class);
            //Colocando nuestra ubicacion
            intent.putExtra("origin_lat", originLatLng.latitude);
            intent.putExtra("origin_lng", originLatLng.longitude);
            intent.putExtra("destination_lat", destinationLatLng.latitude);
            intent.putExtra("destination_lng", destinationLatLng.longitude);
            intent.putExtra("origin", origin);
            intent.putExtra("destination", destination);
            startActivity(intent);
        }else{
            Toast.makeText(this, "Debes de seleccionar el lugar de recogida y de destino", Toast.LENGTH_SHORT).show();
        }
    }

    //Para poder buscar sitios por nuestra zona
    private void limitSearch(){
        LatLng nortSide = SphericalUtil.computeOffset(currentLatLng, 5000, 0);
        LatLng soutSide = SphericalUtil.computeOffset(currentLatLng, 5000, 180);
        //PE seria solo la busqueda en peru, se puede colocar hasta cinco paises
        autocomplete.setCountry("PE");
        autocomplete.setLocationBias(RectangularBounds.newInstance(soutSide, nortSide));

        autocompleteDestination.setCountry("PE");
        autocompleteDestination.setLocationBias(RectangularBounds.newInstance(soutSide, nortSide));
    }

    //Seguir el movimiento del usuario
    private void onCameraMove(){
        cameraIdleListener = new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                try{
                    Geocoder geocoder = new Geocoder(MapClientActivity.this);
                    originLatLng = map.getCameraPosition().target;
                    List<Address> addressList = geocoder.getFromLocation(originLatLng.latitude, originLatLng.longitude, 1);
                    String city = addressList.get(0).getLocality();
                    String address = addressList.get(0).getAddressLine(0);
                    origin = address + " " + city;
                    autocomplete.setText(address + " " + city);
                }catch(Exception e){
                    Log.d("Error", "Ocurrio algo: " + e.getMessage());
                }
            }
        };

    }

    private void instanceAutocompleteOrigin(){
        autocomplete = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.placeAutocompleteOrigin);
        autocomplete.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME));
        autocomplete.setHint("Lugar de recogida");
        autocomplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                origin = place.getName();
                originLatLng = place.getLatLng();
            }

            @Override
            public void onError(@NonNull Status status) {

            }



        });
    }

    private void instanceAutocompleteDestination(){
        autocompleteDestination = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.placeAutocompleteDestination);
        autocompleteDestination.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME));
        autocompleteDestination.setHint("Destino");
        autocompleteDestination.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                destination = place.getName();
                destinationLatLng = place.getLatLng();
            }

            @Override
            public void onError(@NonNull Status status) {

            }
        });
    }


    //Localizar a los conductores que estan cerca de nostros
    private void getActivateDrivers(){
        geofireProvider.getActiveDrivers(currentLatLng, 10).addGeoQueryEventListener(new GeoQueryEventListener(){
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                //Añadir los marcadores de los conductores conectados
                for(Marker marker : driversMarkers){
                    if(marker.getTag() != null){
                        if(marker.getTag().equals(key)){
                            return;
                        }
                    }
                }

                LatLng driverLatLng = new LatLng(location.latitude, location.longitude);
                Marker marker = map.addMarker(new MarkerOptions().position(driverLatLng).title("Conductor disponible").icon(BitmapDescriptorFactory.fromResource(R.drawable.bicitaxi)));
                marker.setTag(key);
                driversMarkers.add(marker);
            }

            @Override
            public void onKeyExited(String key) {
                for(Marker marker : driversMarkers){
                    if(marker.getTag() != null){
                        if(marker.getTag().equals(key)){
                            marker.remove();
                            driversMarkers.remove(marker);
                            return;
                        }
                    }
                }

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                //Actualizar posiciones de los conductores
                for(Marker marker : driversMarkers){
                    if(marker.getTag() != null){
                        if(marker.getTag().equals(key)){
                            marker.setPosition(new LatLng(location.latitude, location.longitude));
                        }
                    }
                }
            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.setOnCameraIdleListener(cameraIdleListener);

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
        if (requestCode == SETTINGS_REQUEST_CODE && gpsActive()) {
            //codigo nuevo en la nueva version de androis alt+enter
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());//activar escuchador de nuestra ubicacion actual
            map.setMyLocationEnabled(true);
        }else if(requestCode == SETTINGS_REQUEST_CODE && !gpsActive()){
            showAlertDialogNOGPS();
        }

    }

    private void showAlertDialogNOGPS(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Por favor activa tu ubicación para continuar")
                .setPositiveButton("Configuraciones", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),SETTINGS_REQUEST_CODE);
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
                                ActivityCompat.requestPermissions(MapClientActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);//habilita permisos
                            }
                        })
                        .create()
                        .show();
            }else{
                ActivityCompat.requestPermissions(MapClientActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);//habilita permisos
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.client_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.action_logout){
            logout();
        }
        if(item.getItemId() == R.id.action_update){
            Intent intent = new Intent(MapClientActivity.this, UpdateProfileActivity.class);
            startActivity(intent);
        }
        if(item.getItemId() == R.id.action_history){
            Intent intent = new Intent(MapClientActivity.this, HistoryBookingClientActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    void logout(){
        authProvider.logout();
        Intent intent = new Intent(MapClientActivity.this, MainActivity.class);
        startActivity(intent);
        finish();//finaliza la actividad
    }

    //Generaremos un token para el mensaje de notificacion
    void generateToken(){
        tokenProvider.create(authProvider.getId());
    }
}