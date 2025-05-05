package com.example.myapplication;

public class Medic {
    private String id;
    private String nume;
    private String prenume;
    private String specialitate;
    private String spital;
    private String program;
    private String telefon;
    private String email;
    private String imagine;
    private String descriere;

    // Constructor gol necesar pentru Firebase
    public Medic() {}

    // Getters și Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNume() { return nume; }
    public void setNume(String nume) { this.nume = nume; }

    public String getPrenume() { return prenume; }
    public void setPrenume(String prenume) { this.prenume = prenume; }

    public String getNumeComplet() { return "Dr. " + prenume + " " + nume; }

    public String getSpecialitate() { return specialitate; }
    public void setSpecialitate(String specialitate) { this.specialitate = specialitate; }

    public String getSpital() { return spital; }
    public void setSpital(String spital) { this.spital = spital; }

    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }

    public String getTelefon() { return telefon; }
    public void setTelefon(String telefon) { this.telefon = telefon; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getImagine() { return imagine; }
    public void setImagine(String imagine) { this.imagine = imagine; }

    public String getDescriere() { return descriere; }
    public void setDescriere(String descriere) { this.descriere = descriere; }
}