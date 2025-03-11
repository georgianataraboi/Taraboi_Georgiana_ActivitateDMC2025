package com.example.test_my_application;

import android.os.Parcel;
import android.os.Parcelable;

enum TipGaraj {
    PUBLIC, PRIVAT, INDUSTRIAL
}

public class garajtransport implements Parcelable {
    private String nume;
    private int capacitate;
    private double suprafata;
    private boolean deschisNonStop;
    private TipGaraj tipGaraj;

    // Constructor
    public garajtransport(String nume, int capacitate, double suprafata, boolean deschisNonStop, TipGaraj tipGaraj) {
        this.nume = nume;
        this.capacitate = capacitate;
        this.suprafata = suprafata;
        this.deschisNonStop = deschisNonStop;
        this.tipGaraj = tipGaraj;
    }

    // Getters
    public String getNume() { return nume; }
    public int getCapacitate() { return capacitate; }
    public double getSuprafata() { return suprafata; }
    public boolean isDeschisNonStop() { return deschisNonStop; }
    public TipGaraj getTipGaraj() { return tipGaraj; }

    // Implementare Parcelable
    protected garajtransport(Parcel in) {
        nume = in.readString();
        capacitate = in.readInt();
        suprafata = in.readDouble();
        deschisNonStop = in.readByte() != 0;
        tipGaraj = TipGaraj.valueOf(in.readString());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(nume);
        dest.writeInt(capacitate);
        dest.writeDouble(suprafata);
        dest.writeByte((byte) (deschisNonStop ? 1 : 0));
        dest.writeString(tipGaraj.name());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<garajtransport> CREATOR = new Creator<garajtransport>() {
        @Override
        public garajtransport createFromParcel(Parcel in) {
            return new garajtransport(in);
        }

        @Override
        public garajtransport[] newArray(int size) {
            return new garajtransport[size];
        }
    };
}
