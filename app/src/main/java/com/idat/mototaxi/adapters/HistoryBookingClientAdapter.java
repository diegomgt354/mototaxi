package com.idat.mototaxi.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.idat.mototaxi.R;
import com.idat.mototaxi.activities.client.HistoryBookingDetailClientActivity;
import com.idat.mototaxi.models.HistoryBooking;
import com.idat.mototaxi.providers.DriverProvider;
import com.squareup.picasso.Picasso;

public class HistoryBookingClientAdapter extends FirebaseRecyclerAdapter<HistoryBooking, HistoryBookingClientAdapter.ViewHolder> {


    private DriverProvider driverProvider;
    private Context mContext;

    public HistoryBookingClientAdapter(FirebaseRecyclerOptions<HistoryBooking> options, Context context){
            super(options);
            driverProvider = new DriverProvider();
            mContext = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull final ViewHolder holder, int position, @NonNull HistoryBooking historyBooking) {
        final String id = getRef(position).getKey();

        holder.textViewOrigin.setText(historyBooking.getOrigin());
        holder.textViewDestination.setText(historyBooking.getDestination());
        holder.textViewCalification.setText(String.valueOf(historyBooking.getCalificationClient()));
        driverProvider.getDriver(historyBooking.getIdDriver()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String name = snapshot.child("name").getValue().toString();
                    holder.textViewName.setText(name);
                    if(snapshot.hasChild("image")){
                        String image = snapshot.child("image").getValue().toString();
                        Picasso.with(mContext).load(image).into(holder.imageViewHistoryBooking);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        holder.viewReceiver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, HistoryBookingDetailClientActivity.class);
                intent.putExtra("idHistoryBooking", id);
                mContext.startActivity(intent);
            }
        });

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_history_booking, parent, false);
        return new ViewHolder(view);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView textViewName;
        private TextView textViewOrigin;
        private TextView textViewDestination;
        private TextView textViewCalification;
        private ImageView imageViewHistoryBooking;
        private View viewReceiver;

        public ViewHolder(View view){
            super(view);
            viewReceiver = view;
            textViewName = view.findViewById(R.id.textViewHistoryName);
            textViewOrigin = view.findViewById(R.id.textViewHistoryOrigin);
            textViewDestination = view.findViewById(R.id.textViewHistoryDestination);
            textViewCalification = view.findViewById(R.id.textViewHistoryCalification);
            imageViewHistoryBooking = view.findViewById(R.id.imgViewHistoryBooking);
        }
    }
}
