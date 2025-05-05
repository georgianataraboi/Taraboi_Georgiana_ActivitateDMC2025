package com.example.myapplication;

public class Spital {
    private String id;
    private String nume;
    private String adresa;
    private String telefon;
    private String website;
    private String imagine;
    private String descriere;
    private String locatie;

    // Constructor gol necesar pentru Firebase
    public Spital() {}

    // Getters și Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNume() { return nume; }
    public void setNume(String nume) { this.nume = nume; }

    public String getAdresa() { return adresa; }
    public void setAdresa(String adresa) { this.adresa = adresa; }

    public String getTelefon() { return telefon; }
    public void setTelefon(String telefon) { this.telefon = telefon; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getImagine() { return imagine; }
    public void setImagine(String imagine) { this.imagine = imagine; }

    public String getDescriere() { return descriere; }
    public void setDescriere(String descriere) { this.descriere = descriere; }

    public String getLocatie() { return locatie; }
    public void setLocatie(String locatie) { this.locatie = locatie; }
}

