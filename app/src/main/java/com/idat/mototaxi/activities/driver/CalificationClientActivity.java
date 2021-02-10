package com.idat.mototaxi.activities.driver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.idat.mototaxi.R;
import com.idat.mototaxi.activities.client.CalificationDriverActivity;
import com.idat.mototaxi.activities.client.MapClientActivity;
import com.idat.mototaxi.models.ClientBooking;
import com.idat.mototaxi.models.HistoryBooking;
import com.idat.mototaxi.providers.ClientBookingProvider;
import com.idat.mototaxi.providers.HistoryBookingProvider;

import java.util.Date;

public class CalificationClientActivity extends AppCompatActivity {


    private TextView textViewOrigin;
    private TextView textViewDestination;
    private TextView textViewPrice;
    private RatingBar ratingBar;
    private Button btnCalification;

    private ClientBookingProvider clientBookingProvider;
    private String clientId;

    private HistoryBooking historyBooking;
    private HistoryBookingProvider historyBookingProvider;

    private float calificationDriver = 0;

    private double extraPrice = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calification_client);

        textViewOrigin = findViewById(R.id.textViewOriginCalificationClient);
        textViewDestination = findViewById(R.id.textViewDestinationCalificationClient);
        ratingBar = findViewById(R.id.ratingbarCalificationClient);
        btnCalification = findViewById(R.id.btnCalificationClient);
        textViewPrice = findViewById(R.id.textViewPriceCalificationClient);

        clientBookingProvider = new ClientBookingProvider();
        historyBookingProvider = new HistoryBookingProvider();

        clientId = getIntent().getStringExtra("idClient");
        extraPrice = getIntent().getDoubleExtra("price", 0);

        textViewPrice.setText("S/. " + String.format("%.1f", extraPrice));

        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float calificacion, boolean fromUser) {
                calificationDriver = calificacion;
            }
        });

        btnCalification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calificate();
            }
        });

        getClientBooking();
    }

    private void getClientBooking(){
        clientBookingProvider.getClientBooking(clientId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    //Obteniendo la informacion del viaje
                    ClientBooking clientBooking = snapshot.getValue(ClientBooking.class);
                    textViewOrigin.setText(clientBooking.getOrigin());
                    textViewDestination.setText(clientBooking.getDestination());

                    historyBooking = new HistoryBooking(
                            clientBooking.getIdHistoryBooking(),
                            clientBooking.getIdClient(),
                            clientBooking.getIdDriver(),
                            clientBooking.getDestination(),
                            clientBooking.getOrigin(),
                            clientBooking.getTime(),
                            clientBooking.getKm(),
                            clientBooking.getStatus(),
                            clientBooking.getOriginLat(),
                            clientBooking.getOriginLng(),
                            clientBooking.getDestinationLat(),
                            clientBooking.getDestinationLng()
                    );

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //Calificando al conductor
    private void calificate() {
        if(calificationDriver > 0){
            historyBooking.setCalificationClient(calificationDriver);
            historyBooking.setTimestamp(new Date().getTime());
            historyBookingProvider.getHistoryBooking(historyBooking.getIdHistoryBooking()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(snapshot.exists()){
                        historyBookingProvider.updateCalificationClient(historyBooking.getIdHistoryBooking(), calificationDriver).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(CalificationClientActivity.this, "La calificacion se guardo correctamente", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(CalificationClientActivity.this, MapDriverActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });;
                    }else{
                        historyBookingProvider.create(historyBooking).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(CalificationClientActivity.this, "La calificacion se guardo correctamente", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(CalificationClientActivity.this, MapDriverActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        }else{
            Toast.makeText(this, "Debes seleccionar una calificacion", Toast.LENGTH_SHORT).show();
        }
    }

}