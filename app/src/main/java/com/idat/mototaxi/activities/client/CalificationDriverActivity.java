package com.idat.mototaxi.activities.client;

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
import com.idat.mototaxi.activities.driver.CalificationClientActivity;
import com.idat.mototaxi.activities.driver.MapDriverActivity;
import com.idat.mototaxi.models.ClientBooking;
import com.idat.mototaxi.models.HistoryBooking;
import com.idat.mototaxi.providers.AuthProvider;
import com.idat.mototaxi.providers.ClientBookingProvider;
import com.idat.mototaxi.providers.HistoryBookingProvider;

import java.util.Date;

public class CalificationDriverActivity extends AppCompatActivity {

    private TextView textViewOrigin;
    private TextView textViewDestination;
    private TextView textViewPrice;
    private RatingBar ratingBar;
    private Button btnCalification;

    private ClientBookingProvider clientBookingProvider;
    private AuthProvider authProvider;

    private HistoryBooking historyBooking;
    private HistoryBookingProvider historyBookingProvider;

    private float calificationClient = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calification_driver);

        textViewOrigin = findViewById(R.id.textViewOriginCalificationDriver);
        textViewDestination = findViewById(R.id.textViewDestinationCalificationDriver);
        ratingBar = findViewById(R.id.ratingbarCalificationDriver);
        btnCalification = findViewById(R.id.btnCalificationDriver);
        textViewPrice = findViewById(R.id.textViewPriceCalificationDriver);

        clientBookingProvider = new ClientBookingProvider();
        historyBookingProvider = new HistoryBookingProvider();
        authProvider = new AuthProvider();

        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float calificacion, boolean fromUser) {
                calificationClient = calificacion;
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
        clientBookingProvider.getClientBooking(authProvider.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    //Obteniendo la informacion del viaje
                    ClientBooking clientBooking = snapshot.getValue(ClientBooking.class);
                    textViewOrigin.setText(clientBooking.getOrigin());
                    textViewPrice.setText("S/. " + String.format("%.1f", clientBooking.getPrice()));
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

    //Calificando al cliente
    private void calificate() {
        if(calificationClient > 0){
            historyBooking.setCalificationDriver(calificationClient);
            historyBooking.setTimestamp(new Date().getTime());
            historyBookingProvider.getHistoryBooking(historyBooking.getIdHistoryBooking()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(snapshot.exists()){
                        historyBookingProvider.updateCalificationDriver(historyBooking.getIdHistoryBooking(), calificationClient).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(CalificationDriverActivity.this, "La calificacion se guardo correctamente", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(CalificationDriverActivity.this, MapClientActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }else{
                        historyBookingProvider.create(historyBooking).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(CalificationDriverActivity.this, "La calificacion se guardo correctamente", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(CalificationDriverActivity.this, MapClientActivity.class);
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