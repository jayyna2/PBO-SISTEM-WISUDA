package model;

public class Berkas {
    private String nim;
    private String fotoPath;
    private String persyaratanPath;
    private String statusVerifikasi;

    public Berkas(String nim, String fotoPath, String persyaratanPath, String statusVerifikasi) {
        this.nim = nim;
        this.fotoPath = fotoPath;
        this.persyaratanPath = persyaratanPath;
        this.statusVerifikasi = statusVerifikasi;
    }

    public String getNim() { return nim; }
    public String getFotoPath() { return fotoPath; }
    public String getPersyaratanPath() { return persyaratanPath; }
    public String getStatusVerifikasi() { return statusVerifikasi; }

    public String toJson() {
        return String.format("{\"fotoPath\":\"%s\",\"syaratPath\":\"%s\",\"statusBerkas\":\"%s\"}",
                escapeJson(fotoPath), escapeJson(persyaratanPath), escapeJson(statusVerifikasi));
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
