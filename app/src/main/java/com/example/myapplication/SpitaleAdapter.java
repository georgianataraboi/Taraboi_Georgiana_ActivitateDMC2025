// SpitaleAdapter.java
package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class SpitaleAdapter extends RecyclerView.Adapter<SpitaleAdapter.SpitalViewHolder> {

    private List<Spital> spitalList;
    private Context context;

    public SpitaleAdapter(List<Spital> spitalList, Context context) {
        this.spitalList = spitalList;
        this.context = context;
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
                    .placeholder(R.drawable.ic_hospital)
                    .error(R.drawable.ic_hospital)
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.ic_hospital);
        }

        // Setează clickListener
        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(context, SpitalDetailActivity.class);
            intent.putExtra("spitalId", spital.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return spitalList.size();
    }

    public static class SpitalViewHolder extends RecyclerView.ViewHolder {
        TextView numeTv, adresaTv;
        ImageView imageView;
        CardView cardView;

        public SpitalViewHolder(@NonNull View itemView) {
            super(itemView);
            numeTv = itemView.findViewById(R.id.spital_nume);
            adresaTv = itemView.findViewById(R.id.spital_adresa);
            imageView = itemView.findViewById(R.id.spital_image);
            cardView = itemView.findViewById(R.id.spital_card);
        }
    }
}