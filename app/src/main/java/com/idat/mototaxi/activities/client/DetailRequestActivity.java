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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.idat.mototaxi.R;
import com.idat.mototaxi.includes.MyToolbar;
import com.idat.mototaxi.models.Info;
import com.idat.mototaxi.providers.GoogleAPIProvider;
import com.idat.mototaxi.providers.InfoProvider;
import com.idat.mototaxi.utils.DecodePoints;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailRequestActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private SupportMapFragment mapFragment;
    private InfoProvider infoProvider;

    private double originLat;
    private double originLng;
    private double destinationLat;
    private double destinationLng;

    private String extraOrigin;
    private String extraDestination;

    private LatLng originLatLng;
    private LatLng destinationLatLng;

    private GoogleAPIProvider googleAPIProvider;

    private List<LatLng> poligonosList;
    private PolylineOptions polylineOptions;

    private TextView textViewOrigin;
    private TextView textViewDestination;
    private TextView textViewTimeAndDistance;
    private TextView textViewPrice;
    private CircleImageView btnBack;

    private Button btnRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_request);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this); // dice que implementa el mapa aqui XD

        //Obteniendo datos de nuestra ubicacion
        originLat = getIntent().getDoubleExtra("origin_lat", 0);
        originLng = getIntent().getDoubleExtra("origin_lng", 0);
        destinationLat = getIntent().getDoubleExtra("destination_lat", 0);
        destinationLng = getIntent().getDoubleExtra("destination_lng", 0);
        extraOrigin = getIntent().getStringExtra("origin");
        extraDestination = getIntent().getStringExtra("destination");

        originLatLng = new LatLng(originLat, originLng);
        destinationLatLng = new LatLng(destinationLat, destinationLng);

        googleAPIProvider = new GoogleAPIProvider(DetailRequestActivity.this);
        infoProvider = new InfoProvider();

        textViewOrigin = findViewById(R.id.textViewOrigin);
        textViewDestination = findViewById(R.id.textViewDestination);
        textViewTimeAndDistance = findViewById(R.id.textViewTime);
        textViewPrice = findViewById(R.id.textViewPrice);
        btnRequest = findViewById(R.id.btnRequestTravel);
        btnBack = findViewById(R.id.circleImgBack);

        textViewOrigin.setText(extraOrigin);
        textViewDestination.setText(extraDestination);

        btnRequest.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                goToRequestDriver();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    private void goToRequestDriver() {
        Intent intent = new Intent(DetailRequestActivity.this, RequestDriverActivity.class);
        intent.putExtra("origin_lat", originLatLng.latitude);
        intent.putExtra("origin_lng", originLatLng.longitude);
        intent.putExtra("destination_lat", destinationLatLng.latitude);
        intent.putExtra("destination_lng", destinationLatLng.longitude);
        intent.putExtra("origin", extraOrigin);
        intent.putExtra("destination", extraDestination);
        startActivity(intent);
        finish();
    }

    //Aca "dibujaremos" la ruta
    private void drawRoute(){
        googleAPIProvider.getDirections(originLatLng, destinationLatLng).enqueue(new Callback<String>() {
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

                    textViewTimeAndDistance.setText(durationText + " " + distanceText);

                    String[] distanceAndKm = distanceText.split(" ");
                    double distanceValue = Double.parseDouble(distanceAndKm[0]);
                    String[] durationAndMin = durationText.split(" ");
                    double durationValue = Double.parseDouble(durationAndMin[0]);

                    calculatePrice(distanceValue, durationValue);

                }catch (Exception e){
                    Log.d("Error", "Ups! Al parecer no funciono: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {

            }
        });
    }

    private void calculatePrice(final double distanceValue, final double durationValue) {
        infoProvider.getInfo().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    Info info = snapshot.getValue(Info.class);
                    double totalDistance = distanceValue * info.getKm();
                    double totalDuration = durationValue * info.getMin();
                    double total = totalDistance + totalDuration;
                    double minTotal = total - 0.5;
                    double maxTotal = total + 0.5;
                    textViewPrice.setText("S/. " + String.format("%.1f", minTotal) +  " - " +  "S/. " + String.format("%.1f", maxTotal));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.getUiSettings().setZoomControlsEnabled(true);

        //Colocando los los pins de mapa
        map.addMarker(new MarkerOptions().position(originLatLng).title("Origen").icon(BitmapDescriptorFactory.fromResource(R.drawable.map_blue)));
        map.addMarker(new MarkerOptions().position(destinationLatLng).title("Destino").icon(BitmapDescriptorFactory.fromResource(R.drawable.map_red)));

        map.animateCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder()
                    .target(originLatLng)
                    .zoom(14f)
                    .build()
        ));

        drawRoute();
    }


}