package com.idat.mototaxi.activities.driver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.idat.mototaxi.R;
import com.idat.mototaxi.providers.AuthProvider;
import com.idat.mototaxi.providers.ClientBookingProvider;
import com.idat.mototaxi.providers.GeofireProvider;

public class NotificationBookingActivity extends AppCompatActivity {

    private TextView textViewDestination;
    private TextView textViewOrigin;
    private TextView textViewMinutes;
    private TextView textViewDistance;
    private TextView textViewCounter;

    private Button btnAccept;
    private Button btnCancel;

    private ClientBookingProvider clientBookingProvider;
    private GeofireProvider geofireProvider;
    private AuthProvider authProvider;

    private String extraIdClient;
    private String extraOrigin;
    private String extraDestination;
    private String extraMin;
    private String extraDistance;

    private MediaPlayer mediaPlayer;
    private ValueEventListener listener;

    private int counter = 10;
    private Handler handler;
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            counter -= 1;
            textViewCounter.setText(String.valueOf(counter));
            if(counter > 0){
                initTimer();
            }else{
                cancelBooking();
            }
        }
    };

    private void initTimer() {
        handler = new Handler();
        handler.postDelayed(runnable, 1000);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_booking);

        textViewDestination = findViewById(R.id.textViewDestinationNotification);
        textViewOrigin = findViewById(R.id.textViewOriginNotification);
        textViewMinutes = findViewById(R.id.textViewMinNotification);
        textViewDistance = findViewById(R.id.textViewDistanceNotification);
        textViewCounter = findViewById(R.id.textViewOriginNotificationCounter);
        btnAccept = findViewById(R.id.btnAcceptBooking);
        btnCancel = findViewById(R.id.btnCancelBooking);

        extraIdClient = getIntent().getStringExtra("idClient");
        extraOrigin = getIntent().getStringExtra("origin");
        extraDestination = getIntent().getStringExtra("destination");
        extraMin = getIntent().getStringExtra("min");
        extraDistance = getIntent().getStringExtra("distance");

        textViewDestination.setText(extraDestination);
        textViewOrigin.setText(extraOrigin);
        textViewDistance.setText(extraDistance);
        textViewMinutes.setText(extraMin);

        mediaPlayer = MediaPlayer.create(this, R.raw.ringtone);
        mediaPlayer.setLooping(true);

        clientBookingProvider = new ClientBookingProvider();
        geofireProvider = new GeofireProvider("active_drivers");
        authProvider = new AuthProvider();


        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        initTimer();
        checkIfClientCancelBooking();

        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptBooking();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelBooking();
            }
        });
    }

    private void checkIfClientCancelBooking(){
        listener = clientBookingProvider.getClientBooking(extraIdClient).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(!snapshot.exists()){
                    Toast.makeText(NotificationBookingActivity.this, "El cliente cancelo el viaje", Toast.LENGTH_LONG).show();
                    if(handler != null) handler.removeCallbacks(runnable);
                    Intent intent = new Intent(NotificationBookingActivity.this, MapDriverActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void cancelBooking() {
        if(handler != null) handler.removeCallbacks(runnable);
        clientBookingProvider.updateStatus(extraIdClient, "cancel");

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(2);
        Intent intent = new Intent(NotificationBookingActivity.this, MapDriverActivity.class);
        startActivity(intent);
        finish();
    }

    private void acceptBooking() {
        if(handler != null) handler.removeCallbacks(runnable);
        geofireProvider.removeLocation(authProvider.getId());
        clientBookingProvider.updateStatus(extraIdClient, "accept");

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(2);

        Intent intent1 = new Intent(NotificationBookingActivity.this, MapDriverBookingActivity.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent1.setAction(Intent.ACTION_RUN);
        intent1.putExtra("idClient", extraIdClient);
        startActivity(intent1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mediaPlayer != null){
            if(mediaPlayer.isPlaying()){
                mediaPlayer.pause();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mediaPlayer != null){
            if(mediaPlayer.isPlaying()){
                mediaPlayer.release();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mediaPlayer != null){
            if(!mediaPlayer.isPlaying()){
                mediaPlayer.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(handler != null) handler.removeCallbacks(runnable);
        if(mediaPlayer != null){
            if(mediaPlayer.isPlaying()){
                mediaPlayer.pause();
            }
        }
        if(listener != null){
            clientBookingProvider.getClientBooking(extraIdClient).removeEventListener(listener);
        }
    }
}
