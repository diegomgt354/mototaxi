package com.idat.mototaxi.activities.driver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.idat.mototaxi.R;
import com.idat.mototaxi.activities.client.HistoryBookingClientActivity;
import com.idat.mototaxi.adapters.HistoryBookingClientAdapter;
import com.idat.mototaxi.adapters.HistoryBookingDriverAdapter;
import com.idat.mototaxi.includes.MyToolbar;
import com.idat.mototaxi.models.HistoryBooking;
import com.idat.mototaxi.providers.AuthProvider;

public class HistoryBookingDriverActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryBookingDriverAdapter adapter;
    private AuthProvider authProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_booking_driver);
        MyToolbar.show(this, "Historial de Viajes", true);

        recyclerView = findViewById(R.id.recyclerViewHistoryBooking);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
    }

    @Override
    protected void onStart() {
        super.onStart();
        authProvider = new AuthProvider();
        Query query = FirebaseDatabase.getInstance().getReference()
                .child("HistoryBooking")
                .orderByChild("idDriver")
                .equalTo(authProvider.getId());
        FirebaseRecyclerOptions<HistoryBooking> options = new FirebaseRecyclerOptions.Builder<HistoryBooking>()
                .setQuery(query, HistoryBooking.class).build();

        adapter = new HistoryBookingDriverAdapter(options, HistoryBookingDriverActivity.this);
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }
}