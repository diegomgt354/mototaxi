package com.idat.mototaxi.activities.client;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.idat.mototaxi.R;
import com.idat.mototaxi.models.HistoryBooking;
import com.idat.mototaxi.providers.DriverProvider;
import com.idat.mototaxi.providers.HistoryBookingProvider;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class HistoryBookingDetailClientActivity extends AppCompatActivity {

    private TextView textViewName;
    private TextView textViewOrigin;
    private TextView textViewDestination;
    private TextView textViewYourCalification;
    private RatingBar ratingBarCalification;
    private CircleImageView circleImageView;
    private CircleImageView btnBack;

    private String extraId;
    private HistoryBookingProvider historyBookingProvider;
    private DriverProvider driverProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_booking_detail_client);

        textViewName = findViewById(R.id.textViewNameBookingDetail);
        textViewOrigin = findViewById(R.id.textViewOriginHistoryBookingDetail);
        textViewDestination = findViewById(R.id.textViewDestinationHistoryBookingDetail);
        textViewYourCalification = findViewById(R.id.textViewCalificationHistoryBookingDetail);
        ratingBarCalification = findViewById(R.id.ratingbarHistoryBookingDetail);
        circleImageView = findViewById(R.id.circleImgHistoryBookingDetail);
        btnBack = findViewById(R.id.circleImgBack);

        extraId = getIntent().getStringExtra("idHistoryBooking");
        historyBookingProvider = new HistoryBookingProvider();
        driverProvider = new DriverProvider();
        getHistoryBooking();

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void getHistoryBooking() {
        historyBookingProvider.getHistoryBooking(extraId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    HistoryBooking historyBooking = snapshot.getValue(HistoryBooking.class);
                    textViewOrigin.setText(historyBooking.getOrigin());
                    textViewDestination.setText(historyBooking.getDestination());
                    textViewYourCalification.setText("Tu calificacion: " + historyBooking.getCalificationDriver());
                    if(snapshot.hasChild("calificationClient")){
                        ratingBarCalification.setRating((float) historyBooking.getCalificationClient());
                    }
                    driverProvider.getDriver(historyBooking.getIdDriver()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists()){
                                String name = snapshot.child("name").getValue().toString();
                                textViewName.setText(name.toUpperCase());
                                if(snapshot.hasChild("image")){
                                    String image = snapshot.child("image").getValue().toString();
                                    Picasso.with(HistoryBookingDetailClientActivity.this).load(image).into(circleImageView);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}