package model;

import java.util.Locale;

public class Mahasiswa extends User {
    private String email;
    private String telepon;
    private String fakultas;
    private String prodi;
    private double ipk;
    private String regStatus; // registration status (optional, for listings)

    public Mahasiswa(String nim, String nama, String password, String email, String telepon, String fakultas, String prodi, double ipk) {
        super(nim, password, nama, "mahasiswa");
        this.email = email;
        this.telepon = telepon;
        this.fakultas = fakultas;
        this.prodi = prodi;
        this.ipk = ipk;
    }

    public String getNim() { return username; }
    public void setNim(String nim) { this.username = nim; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelepon() { return telepon; }
    public void setTelepon(String telepon) { this.telepon = telepon; }

    public String getFakultas() { return fakultas; }
    public void setFakultas(String fakultas) { this.fakultas = fakultas; }

    public String getProdi() { return prodi; }
    public void setProdi(String prodi) { this.prodi = prodi; }

    public double getIpk() { return ipk; }
    public void setIpk(double ipk) { this.ipk = ipk; }

    public String getRegStatus() { return regStatus; }
    public void setRegStatus(String regStatus) { this.regStatus = regStatus; }

    @Override
    public String toJson() {
        return String.format(Locale.US,
                "{\"nim\":\"%s\",\"nama\":\"%s\",\"email\":\"%s\",\"telepon\":\"%s\",\"fakultas\":\"%s\",\"prodi\":\"%s\",\"ipk\":%.2f%s}",
                escapeJson(username), escapeJson(nama), escapeJson(email), escapeJson(telepon),
                escapeJson(fakultas), escapeJson(prodi), ipk,
                (regStatus != null ? String.format(",\"regStatus\":\"%s\"", regStatus) : ""));
    }

    private String escapeJson(String val) {
        if (val == null) return "";
        return val.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
