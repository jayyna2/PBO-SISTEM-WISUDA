package model;

import java.util.Locale;

public class Pembayaran {
    private String nim;
    private double jumlahBayar;
    private String buktiBayarPath;
    private String statusPembayaran;
    private String tanggalBayar;

    public Pembayaran(String nim, double jumlahBayar, String buktiBayarPath, String statusPembayaran, String tanggalBayar) {
        this.nim = nim;
        this.jumlahBayar = jumlahBayar;
        this.buktiBayarPath = buktiBayarPath;
        this.statusPembayaran = statusPembayaran;
        this.tanggalBayar = tanggalBayar;
    }

    public String getNim() { return nim; }
    public double getJumlahBayar() { return jumlahBayar; }
    public String getBuktiBayarPath() { return buktiBayarPath; }
    public String getStatusPembayaran() { return statusPembayaran; }
    public String getTanggalBayar() { return tanggalBayar; }

    public String toJson() {
        return String.format(Locale.US, "{\"buktiPath\":\"%s\",\"statusPembayaran\":\"%s\",\"jumlahBayar\":%.2f}",
                escapeJson(buktiBayarPath), escapeJson(statusPembayaran), jumlahBayar);
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
