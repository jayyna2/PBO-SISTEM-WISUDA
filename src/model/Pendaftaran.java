package model;

import java.util.Locale;

public class Pendaftaran {
    private int idPendaftaran;
    private Mahasiswa mahasiswa;
    private String periodeWisuda;
    private String judulSkripsi;
    private String tanggalDaftar;
    private String status;
    private String keterangan;
    private Berkas berkas;
    private Pembayaran pembayaran;

    public Pendaftaran(int idPendaftaran, Mahasiswa mahasiswa, String periodeWisuda, String judulSkripsi, String tanggalDaftar, String status, String keterangan) {
        this.idPendaftaran = idPendaftaran;
        this.mahasiswa = mahasiswa;
        this.periodeWisuda = periodeWisuda;
        this.judulSkripsi = judulSkripsi;
        this.tanggalDaftar = tanggalDaftar;
        this.status = status;
        this.keterangan = keterangan;
    }

    public int getIdPendaftaran() { return idPendaftaran; }
    public Mahasiswa getMahasiswa() { return mahasiswa; }
    public String getPeriodeWisuda() { return periodeWisuda; }
    public String getJudulSkripsi() { return judulSkripsi; }
    public String getTanggalDaftar() { return tanggalDaftar; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    public Berkas getBerkas() { return berkas; }
    public void setBerkas(Berkas berkas) { this.berkas = berkas; }

    public Pembayaran getPembayaran() { return pembayaran; }
    public void setPembayaran(Pembayaran pembayaran) { this.pembayaran = pembayaran; }

    public String toJson() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"registered\":true,");
        sb.append("\"nim\":\"").append(escapeJson(mahasiswa.getNim())).append("\",");
        sb.append("\"nama\":\"").append(escapeJson(mahasiswa.getNama())).append("\",");
        sb.append("\"email\":\"").append(escapeJson(mahasiswa.getEmail())).append("\",");
        sb.append("\"telepon\":\"").append(escapeJson(mahasiswa.getTelepon())).append("\",");
        sb.append("\"fakultas\":\"").append(escapeJson(mahasiswa.getFakultas())).append("\",");
        sb.append("\"prodi\":\"").append(escapeJson(mahasiswa.getProdi())).append("\",");
        sb.append(String.format(Locale.US, "\"ipk\":%.2f,", mahasiswa.getIpk()));
        sb.append("\"periode\":\"").append(escapeJson(periodeWisuda)).append("\",");
        sb.append("\"judul\":\"").append(escapeJson(judulSkripsi)).append("\",");
        sb.append("\"tanggal\":\"").append(escapeJson(tanggalDaftar)).append("\",");
        sb.append("\"status\":\"").append(escapeJson(status)).append("\",");
        sb.append("\"keterangan\":\"").append(escapeJson(keterangan)).append("\",");
        
        if (berkas != null) {
            sb.append("\"fotoPath\":\"").append(escapeJson(berkas.getFotoPath())).append("\",");
            sb.append("\"syaratPath\":\"").append(escapeJson(berkas.getPersyaratanPath())).append("\",");
            sb.append("\"statusBerkas\":\"").append(escapeJson(berkas.getStatusVerifikasi())).append("\",");
        } else {
            sb.append("\"statusBerkas\":\"PENDING\",");
        }

        if (pembayaran != null) {
            sb.append("\"buktiPath\":\"").append(escapeJson(pembayaran.getBuktiBayarPath())).append("\",");
            sb.append("\"statusPembayaran\":\"").append(escapeJson(pembayaran.getStatusPembayaran())).append("\"");
        } else {
            sb.append("\"statusPembayaran\":\"PENDING\"");
        }
        sb.append("}");
        return sb.toString();
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
