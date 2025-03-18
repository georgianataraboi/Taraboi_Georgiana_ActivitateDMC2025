package com.example.test_my_application;

import android.os.Parcel;
import android.os.Parcelable;
import java.time.LocalTime;

enum TipGaraj {
    PUBLIC, PRIVAT, INDUSTRIAL
}

public class garajtransport implements Parcelable {
    private String nume;
    private int capacitate;
    private double suprafata;
    private boolean deschisNonStop;
    private TipGaraj tipGaraj;

    private LocalTime oraDeschidere;

    // Constructor
    public garajtransport(String nume, int capacitate, double suprafata, boolean deschisNonStop, TipGaraj tipGaraj, LocalTime oraDeschidere) {
        this.nume = nume;
        this.capacitate = capacitate;
        this.suprafata = suprafata;
        this.deschisNonStop = deschisNonStop;
        this.tipGaraj = tipGaraj;
        this.oraDeschidere = oraDeschidere;
    }

    // Getters
    public String getNume() { return nume; }
    public int getCapacitate() { return capacitate; }
    public double getSuprafata() { return suprafata; }
    public boolean isDeschisNonStop() { return deschisNonStop; }
    public TipGaraj getTipGaraj() { return tipGaraj; }

    public LocalTime getOraDeschidere() { return oraDeschidere; }

    //    pt listview
    @Override
    public String toString() {
        return nume + " - " + tipGaraj + " - " + capacitate + " locuri - " +
                (deschisNonStop ? "Non-Stop" : "Deschis la: " + oraDeschidere);
    }

    // Implementare Parcelable
    protected garajtransport(Parcel in) {
        nume = in.readString();
        capacitate = in.readInt();
        suprafata = in.readDouble();
        deschisNonStop = in.readByte() != 0;
        tipGaraj = TipGaraj.valueOf(in.readString());
        String ora = in.readString();
        oraDeschidere = deschisNonStop ? LocalTime.of(0, 0) : LocalTime.parse(ora);
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(nume);
        dest.writeInt(capacitate);
        dest.writeDouble(suprafata);
        dest.writeByte((byte) (deschisNonStop ? 1 : 0));
        dest.writeString(tipGaraj.name());
        dest.writeString(oraDeschidere.toString());
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
