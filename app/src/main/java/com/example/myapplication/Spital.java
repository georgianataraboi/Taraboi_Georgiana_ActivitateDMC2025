package com.example.myapplication;

public class Spital {
    private String id;
    private String nume;
    private String adresa;
    private String telefon;
    private String descriere;
    private String email;
    private String website;
    private String imagine;
    private String locatie;
    private Long firebase_last_updated;
    private Long local_last_updated;

    public Spital() {
        // Constructor gol necesar pentru Firebase
    }

    public Spital(String nume, String adresa, String telefon, String descriere,
                  String email, String website, String imagine, String locatie) {
        this.nume = nume;
        this.adresa = adresa;
        this.telefon = telefon;
        this.descriere = descriere;
        this.email = email;
        this.website = website;
        this.imagine = imagine;
        this.locatie = locatie;
    }

    // Getters
    public String getId() { return id; }
    public String getNume() { return nume; }
    public String getAdresa() { return adresa; }
    public String getTelefon() { return telefon; }
    public String getDescriere() { return descriere; }
    public String getEmail() { return email; }
    public String getWebsite() { return website; }
    public String getImagine() { return imagine; }
    public String getLocatie() { return locatie; }
    public Long getFirebase_last_updated() { return firebase_last_updated; }
    public Long getLocal_last_updated() { return local_last_updated; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setNume(String nume) { this.nume = nume; }
    public void setAdresa(String adresa) { this.adresa = adresa; }
    public void setTelefon(String telefon) { this.telefon = telefon; }
    public void setDescriere(String descriere) { this.descriere = descriere; }
    public void setEmail(String email) { this.email = email; }
    public void setWebsite(String website) { this.website = website; }
    public void setImagine(String imagine) { this.imagine = imagine; }
    public void setLocatie(String locatie) { this.locatie = locatie; }
    public void setFirebase_last_updated(Long firebase_last_updated) { this.firebase_last_updated = firebase_last_updated; }
    public void setLocal_last_updated(Long local_last_updated) { this.local_last_updated = local_last_updated; }

    // Helper methods
    public double getLatitude() {
        if (locatie != null && locatie.contains(",")) {
            return Double.parseDouble(locatie.split(",")[0]);
        }
        return 0.0;
    }

    public double getLongitude() {
        if (locatie != null && locatie.contains(",")) {
            return Double.parseDouble(locatie.split(",")[1]);
        }
        return 0.0;
    }

    @Override
    public String toString() {
        return "Spital{" +
                "id='" + id + '\'' +
                ", nume='" + nume + '\'' +
                ", adresa='" + adresa + '\'' +
                ", telefon='" + telefon + '\'' +
                '}';
    }
}