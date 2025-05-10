package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class SpitaleAdapter extends RecyclerView.Adapter<SpitaleAdapter.SpitalViewHolder> {

    private List<Spital> spitalList;
    private Context context;
    private RequestOptions glideOptions;

    public SpitaleAdapter(List<Spital> spitalList, Context context) {
        this.spitalList = spitalList;
        this.context = context;

        // Pre-configure Glide options for better performance
        this.glideOptions = new RequestOptions()
                .placeholder(R.drawable.ic_hospital)
                .error(R.drawable.ic_hospital)
                .centerCrop();
    }

    @NonNull
    @Override
    public SpitalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_spital, parent, false);
        return new SpitalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SpitalViewHolder holder, int position) {
        Spital spital = spitalList.get(position);

        holder.numeTv.setText(spital.getNume());
        holder.adresaTv.setText(spital.getAdresa());

        // Încarcă imaginea cu Glide dacă este disponibilă
        if (spital.getImagine() != null && !spital.getImagine().isEmpty()) {
            Glide.with(context)
                    .load(spital.getImagine())
                    .apply(glideOptions)
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.ic_hospital);
        }

        // Setează clickListener pe întregul card
        holder.cardView.setOnClickListener(v -> openHospitalDetails(spital));

        // Adaugă și click listener pentru butonul "Vezi"
        if (holder.veziDetailsTv != null) {
            holder.veziDetailsTv.setOnClickListener(v -> openHospitalDetails(spital));
        }
    }

    private void openHospitalDetails(Spital spital) {
        // Salvează ultimul spital vizitat în SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("GeoMedUserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("lastVisitedHospital", spital.getNume());
        editor.putBoolean("hasVisitedHospital", true);
        editor.apply();

        // Navigare către detalii
        Intent intent = new Intent(context, SpitalDetailActivity.class);
        intent.putExtra("spitalId", spital.getId());
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return spitalList.size();
    }

    public static class SpitalViewHolder extends RecyclerView.ViewHolder {
        TextView numeTv, adresaTv, veziDetailsTv;
        ImageView imageView;
        CardView cardView;

        public SpitalViewHolder(@NonNull View itemView) {
            super(itemView);
            numeTv = itemView.findViewById(R.id.spital_nume);
            adresaTv = itemView.findViewById(R.id.spital_adresa);
            imageView = itemView.findViewById(R.id.spital_image);
            cardView = itemView.findViewById(R.id.spital_card);
            veziDetailsTv = itemView.findViewById(R.id.vezi_detalii);
        }
    }

    public void updateList(List<Spital> newList) {
        this.spitalList = newList;
        notifyDataSetChanged();
    }
}