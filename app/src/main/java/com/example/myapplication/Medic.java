package com.example.myapplication;

public class Medic {
    private String id;
    private String nume;
    private String prenume;
    private String specialitate;
    private String spital;
    private String telefon;
    private String email;
    private String program;
    private String imagine;
    private String descriere;
    private Long firebase_last_updated;
    private Long local_last_updated;

    public Medic() {
        // Constructor gol necesar pentru Firebase
    }

    public Medic(String nume, String prenume, String specialitate, String spital,
                 String telefon, String email, String program, String imagine, String descriere) {
        this.nume = nume;
        this.prenume = prenume;
        this.specialitate = specialitate;
        this.spital = spital;
        this.telefon = telefon;
        this.email = email;
        this.program = program;
        this.imagine = imagine;
        this.descriere = descriere;
    }

    // Getters
    public String getId() { return id; }
    public String getNume() { return nume; }
    public String getPrenume() { return prenume; }
    public String getSpecialitate() { return specialitate; }
    public String getSpital() { return spital; }
    public String getTelefon() { return telefon; }
    public String getEmail() { return email; }
    public String getProgram() { return program; }
    public String getImagine() { return imagine; }
    public String getDescriere() { return descriere; }
    public Long getFirebase_last_updated() { return firebase_last_updated; }
    public Long getLocal_last_updated() { return local_last_updated; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setNume(String nume) { this.nume = nume; }
    public void setPrenume(String prenume) { this.prenume = prenume; }
    public void setSpecialitate(String specialitate) { this.specialitate = specialitate; }
    public void setSpital(String spital) { this.spital = spital; }
    public void setTelefon(String telefon) { this.telefon = telefon; }
    public void setEmail(String email) { this.email = email; }
    public void setProgram(String program) { this.program = program; }
    public void setImagine(String imagine) { this.imagine = imagine; }
    public void setDescriere(String descriere) { this.descriere = descriere; }
    public void setFirebase_last_updated(Long firebase_last_updated) { this.firebase_last_updated = firebase_last_updated; }
    public void setLocal_last_updated(Long local_last_updated) { this.local_last_updated = local_last_updated; }

    // Helper method
    public String getNumeComplet() {
        return "Dr. " + prenume + " " + nume;
    }

    @Override
    public String toString() {
        return "Medic{" +
                "id='" + id + '\'' +
                ", nume='" + nume + '\'' +
                ", prenume='" + prenume + '\'' +
                ", specialitate='" + specialitate + '\'' +
                ", spital='" + spital + '\'' +
                '}';
    }
}