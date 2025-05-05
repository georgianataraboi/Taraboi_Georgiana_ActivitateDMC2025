// MediciAdapter.java
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

public class MediciAdapter extends RecyclerView.Adapter<MediciAdapter.MedicViewHolder> {

    private List<Medic> medicList;
    private Context context;

    public MediciAdapter(List<Medic> medicList, Context context) {
        this.medicList = medicList;
        this.context = context;
    }

    @NonNull
    @Override
    public MedicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medic, parent, false);
        return new MedicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicViewHolder holder, int position) {
        Medic medic = medicList.get(position);

        holder.numeTv.setText(medic.getNumeComplet());
        holder.specialitateTv.setText(medic.getSpecialitate());
        holder.spitalTv.setText(medic.getSpital());

        // Încarcă imaginea cu Glide dacă este disponibilă
        if (medic.getImagine() != null && !medic.getImagine().isEmpty()) {
            Glide.with(context)
                    .load(medic.getImagine())
                    .placeholder(R.drawable.ic_doctor)
                    .error(R.drawable.ic_doctor)
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_doctor);
        }

        // Setează clickListener
        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MedicDetailActivity.class);
            intent.putExtra("medicId", medic.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return medicList.size();
    }

    public static class MedicViewHolder extends RecyclerView.ViewHolder {
        TextView numeTv, specialitateTv, spitalTv, veziProfilTv;
        ImageView profileImage;
        CardView cardView;

        public MedicViewHolder(@NonNull View itemView) {
            super(itemView);
            numeTv = itemView.findViewById(R.id.medic_nume);
            specialitateTv = itemView.findViewById(R.id.medic_specialitate);
            spitalTv = itemView.findViewById(R.id.medic_spital);
            profileImage = itemView.findViewById(R.id.medic_image);
            veziProfilTv = itemView.findViewById(R.id.vezi_profil);
            cardView = itemView.findViewById(R.id.medic_card);
        }
    }
}