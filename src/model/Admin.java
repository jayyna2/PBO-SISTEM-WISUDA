package model;

public class Admin extends User {
    public Admin(String username, String password, String namaLengkap) {
        super(username, password, namaLengkap, "admin");
    }

    @Override
    public String toJson() {
        return String.format("{\"success\":true,\"role\":\"admin\",\"nama\":\"%s\"}", escapeJson(nama));
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
