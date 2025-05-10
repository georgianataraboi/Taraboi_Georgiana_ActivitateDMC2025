package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class MediciGridAdapter extends RecyclerView.Adapter<MediciGridAdapter.MedicViewHolder> {

    private List<Medic> mediciList;
    private Context context;
    private RequestOptions glideOptions;

    public MediciGridAdapter(List<Medic> mediciList, Context context) {
        this.mediciList = mediciList;
        this.context = context;

        // Pre-configure Glide options for better performance
        this.glideOptions = new RequestOptions()
                .placeholder(R.drawable.ic_doctor)
                .error(R.drawable.ic_doctor)
                .circleCrop();
    }

    @NonNull
    @Override
    public MedicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medic_grid, parent, false);
        return new MedicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicViewHolder holder, int position) {
        Medic medic = mediciList.get(position);

        if (medic != null) {
            holder.numeTv.setText(medic.getNume());

            // Verifică dacă avem specialitate
            if (medic.getSpecialitate() != null && !medic.getSpecialitate().isEmpty()) {
                holder.specialitateTv.setText(medic.getSpecialitate());
                holder.specialitateTv.setVisibility(View.VISIBLE);
            } else {
                holder.specialitateTv.setVisibility(View.GONE);
            }

            // Încarcă imaginea cu Glide dacă este disponibilă
            if (medic.getImagine() != null && !medic.getImagine().isEmpty()) {
                Glide.with(context)
                        .load(medic.getImagine())
                        .apply(glideOptions)
                        .into(holder.imageView);
            } else {
                holder.imageView.setImageResource(R.drawable.ic_doctor);
            }

            // Setează clickListener pentru card și butonul de detalii
            View.OnClickListener clickListener = v -> {
                Intent intent = new Intent(context, MedicDetailActivity.class);
                intent.putExtra("medicId", medic.getId());
                context.startActivity(intent);
            };

            holder.cardView.setOnClickListener(clickListener);
            holder.veziDetaliiBtn.setOnClickListener(clickListener);
        }
    }

    @Override
    public int getItemCount() {
        return mediciList.size();
    }

    public static class MedicViewHolder extends RecyclerView.ViewHolder {
        TextView numeTv, specialitateTv;
        ImageView imageView;
        CardView cardView;
        Button veziDetaliiBtn;

        public MedicViewHolder(@NonNull View itemView) {
            super(itemView);
            numeTv = itemView.findViewById(R.id.medic_nume);
            specialitateTv = itemView.findViewById(R.id.medic_specialitate);
            imageView = itemView.findViewById(R.id.medic_image);
            cardView = itemView.findViewById(R.id.medic_card);
            veziDetaliiBtn = itemView.findViewById(R.id.vezi_detalii_btn);
        }
    }
}