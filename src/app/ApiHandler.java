package app;

import database.DbHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import model.*;

public class ApiHandler implements HttpHandler {
    private final DbHelper db = new DbHelper();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // CORS headers
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(244, -1);
            return;
        }

        try {
            if (path.equals("/api/login") && "POST".equalsIgnoreCase(method)) {
                handleLogin(exchange);
            } else if (path.equals("/api/mahasiswa/profil") && "GET".equalsIgnoreCase(method)) {
                handleGetProfil(exchange);
            } else if (path.equals("/api/mahasiswa/daftar") && "POST".equalsIgnoreCase(method)) {
                handleDaftar(exchange);
            } else if (path.equals("/api/mahasiswa/upload") && "POST".equalsIgnoreCase(method)) {
                handleUpload(exchange);
            } else if (path.equals("/api/mahasiswa/status") && "GET".equalsIgnoreCase(method)) {
                handleStatus(exchange);
            } else if (path.equals("/api/admin/stats") && "GET".equalsIgnoreCase(method)) {
                handleAdminStats(exchange);
            } else if (path.equals("/api/admin/pendaftar") && "GET".equalsIgnoreCase(method)) {
                handleAdminPendaftar(exchange);
            } else if (path.equals("/api/admin/verify") && "POST".equalsIgnoreCase(method)) {
                handleVerify(exchange);
            } else if (path.equals("/api/admin/mhs") && "GET".equalsIgnoreCase(method)) {
                handleGetStudents(exchange);
            } else if (path.equals("/api/admin/mhs") && "POST".equalsIgnoreCase(method)) {
                handleSaveStudent(exchange);
            } else if (path.equals("/api/admin/mhs/delete") && "POST".equalsIgnoreCase(method)) {
                handleDeleteStudent(exchange);
            } else if (path.equals("/api/admin/export") && "GET".equalsIgnoreCase(method)) {
                handleExportCsv(exchange);
            } else {
                sendError(exchange, 404, "Endpoint not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Server Error: " + e.getMessage());
        }
    }

    // --- API Handlers ---

    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        String role = getJsonValue(body, "role");
        String username = getJsonValue(body, "username");
        String password = getJsonValue(body, "password");

        User user = db.login(username, password, role);
        if (user != null) {
            if (user instanceof Mahasiswa) {
                sendJson(exchange, 200, String.format("{\"success\":true,\"role\":\"mahasiswa\",\"nim\":\"%s\",\"nama\":\"%s\"}",
                        user.getUsername().replace("\"", "\\\""), user.getNama().replace("\"", "\\\"")));
            } else {
                sendJson(exchange, 200, user.toJson());
            }
        } else {
            sendJson(exchange, 200, "{\"success\":false,\"error\":\"Username/NIM atau password salah.\"}");
        }
    }

    private void handleGetProfil(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
        String nim = query.get("nim");
        if (nim == null) {
            sendError(exchange, 400, "Parameter 'nim' is required");
            return;
        }
        Mahasiswa result = db.getMahasiswaProfile(nim);
        if (result != null) {
            sendJson(exchange, 200, result.toJson());
        } else {
            sendJson(exchange, 404, "{\"error\":\"Profil mahasiswa tidak ditemukan.\"}");
        }
    }

    private void handleDaftar(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        String nim = getJsonValue(body, "nim");
        String periode = getJsonValue(body, "periode");
        String judul = getJsonValue(body, "judul");

        if (nim == null || periode == null || judul == null) {
            sendError(exchange, 400, "Missing required parameters");
            return;
        }

        String result = db.registerWisuda(nim, periode, judul);
        sendJson(exchange, 200, result);
    }

    private void handleUpload(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        String nim = getJsonValue(body, "nim");
        String fotoData = getJsonValue(body, "foto");
        String syaratData = getJsonValue(body, "syarat");
        String buktiData = getJsonValue(body, "bukti");

        if (nim == null) {
            sendError(exchange, 400, "NIM parameter is required");
            return;
        }

        try {
            String fotoPath = saveBase64File(nim, fotoData, "foto");
            String syaratPath = saveBase64File(nim, syaratData, "syarat");
            String buktiPath = saveBase64File(nim, buktiData, "bukti");

            boolean success = db.saveUploadedFiles(nim, fotoPath, syaratPath, buktiPath);
            if (success) {
                sendJson(exchange, 200, "{\"success\":true,\"message\":\"Berkas berhasil diupload.\"}");
            } else {
                sendJson(exchange, 500, "{\"success\":false,\"error\":\"Gagal menyimpan data upload ke database.\"}");
            }
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
        String nim = query.get("nim");
        if (nim == null) {
            sendError(exchange, 400, "Parameter 'nim' is required");
            return;
        }
        Pendaftaran pend = db.getPendaftaran(nim);
        if (pend != null) {
            sendJson(exchange, 200, pend.toJson());
        } else {
            sendJson(exchange, 200, "{\"registered\":false,\"status\":\"BELUM_DAFTAR\"}");
        }
    }

    private void handleAdminStats(HttpExchange exchange) throws IOException {
        String result = db.getAdminStats();
        sendJson(exchange, 200, result);
    }

    private void handleAdminPendaftar(HttpExchange exchange) throws IOException {
        List<Pendaftaran> list = db.getAdminPendaftar();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i).toJson());
        }
        sb.append("]");
        sendJson(exchange, 200, sb.toString());
    }

    private void handleVerify(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        String nim = getJsonValue(body, "nim");
        String type = getJsonValue(body, "type"); // "berkas", "pembayaran", "pendaftaran"
        String status = getJsonValue(body, "status"); // APPROVED, REJECTED
        String remarks = getJsonValue(body, "remarks"); // Catatan verifikator

        if (nim == null || type == null || status == null) {
            sendError(exchange, 400, "Missing parameters");
            return;
        }

        String result = db.verify(nim, type, status, remarks);
        sendJson(exchange, 200, result);
    }

    private void handleGetStudents(HttpExchange exchange) throws IOException {
        List<Mahasiswa> list = db.getStudents();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i).toJson());
        }
        sb.append("]");
        sendJson(exchange, 200, sb.toString());
    }

    private void handleSaveStudent(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        String nim = getJsonValue(body, "nim");
        String nama = getJsonValue(body, "nama");
        String pwd = getJsonValue(body, "password");
        String email = getJsonValue(body, "email");
        String telp = getJsonValue(body, "telepon");
        String fak = getJsonValue(body, "fakultas");
        String prodi = getJsonValue(body, "prodi");
        String ipkStr = getJsonValue(body, "ipk");
        String isEditStr = getJsonValue(body, "isEdit"); // "true" or "false"

        if (nim == null || nama == null || email == null || telp == null || fak == null || prodi == null || ipkStr == null) {
            sendError(exchange, 400, "Missing student field parameters");
            return;
        }

        double ipk;
        try {
            ipk = Double.parseDouble(ipkStr);
        } catch (NumberFormatException e) {
            sendJson(exchange, 400, "{\"success\":false,\"error\":\"Format IPK tidak valid.\"}");
            return;
        }

        boolean isEdit = "true".equalsIgnoreCase(isEditStr);
        String result;
        if (isEdit) {
            result = db.updateStudent(nim, nama, pwd, email, telp, fak, prodi, ipk);
        } else {
            result = db.addStudent(nim, nama, pwd, email, telp, fak, prodi, ipk);
        }
        sendJson(exchange, 200, result);
    }

    private void handleDeleteStudent(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        String nim = getJsonValue(body, "nim");
        if (nim == null) {
            sendError(exchange, 400, "NIM parameter is required");
            return;
        }
        String result = db.deleteStudent(nim);
        sendJson(exchange, 200, result);
    }

    private void handleExportCsv(HttpExchange exchange) throws IOException {
        List<Pendaftaran> list = db.getAdminPendaftar();
        
        StringBuilder csv = new StringBuilder();
        csv.append("NIM,Nama Lengkap,Email,Telepon,Fakultas,Program Studi,IPK,Periode Wisuda,Judul Skripsi,Status Pendaftaran,Tanggal Daftar\n");
        
        for (Pendaftaran p : list) {
            Mahasiswa m = p.getMahasiswa();
            csv.append(escapeCsv(m.getNim())).append(",")
               .append(escapeCsv(m.getNama())).append(",")
               .append(escapeCsv(m.getEmail())).append(",")
               .append(escapeCsv(m.getTelepon())).append(",")
               .append(escapeCsv(m.getFakultas())).append(",")
               .append(escapeCsv(m.getProdi())).append(",")
               .append(String.format(Locale.US, "%.2f", m.getIpk())).append(",")
               .append(escapeCsv(p.getPeriodeWisuda())).append(",")
               .append(escapeCsv(p.getJudulSkripsi())).append(",")
               .append(escapeCsv(p.getStatus())).append(",")
               .append(escapeCsv(p.getTanggalDaftar())).append("\n");
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=utf-8");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"rekap_wisudawan.csv\"");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // --- Helpers ---

    private String saveBase64File(String nim, String dataUri, String type) throws IOException {
        if (dataUri == null || dataUri.trim().isEmpty() || !dataUri.contains("base64,")) {
            return null;
        }

        int commaIdx = dataUri.indexOf("base64,");
        String base64 = dataUri.substring(commaIdx + 7).trim();
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Dekoding file " + type + " gagal: Base64 data rusak.");
        }

        String ext = "bin";
        if (dataUri.contains("image/png")) ext = "png";
        else if (dataUri.contains("image/jpeg") || dataUri.contains("image/jpg")) ext = "jpg";
        else if (dataUri.contains("application/pdf")) ext = "pdf";

        String filename = nim + "_" + type + "." + ext;
        
        File uploadsDir = null;
        File[] checkDirs = {
            new File("web/uploads"),
            new File("../web/uploads")
        };
        
        for (File dir : checkDirs) {
            if (dir.exists() && dir.isDirectory()) {
                uploadsDir = dir;
                break;
            }
        }
        
        if (uploadsDir == null) {
            uploadsDir = new File("web/uploads");
            if (!uploadsDir.exists()) {
                uploadsDir.mkdirs();
            }
        }

        File file = new File(uploadsDir, filename);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
        }

        return "uploads/" + filename;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
            bos.write(buffer, 0, read);
        }
        return bos.toString(StandardCharsets.UTF_8.name());
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String param : query.split("&")) {
            String[] parts = param.split("=");
            if (parts.length > 1) {
                try {
                    map.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8.name()),
                            URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name()));
                } catch (Exception e) {}
            }
        }
        return map;
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendError(HttpExchange exchange, int status, String msg) throws IOException {
        String json = String.format("{\"success\":false,\"error\":\"%s\"}", msg.replace("\"", "\\\""));
        sendJson(exchange, status, json);
    }

    private String getJsonValue(String json, String key) {
        if (json == null) return null;
        String quoteKey = "\"" + key + "\"";
        int idx = json.indexOf(quoteKey);
        if (idx == -1) return null;
        idx += quoteKey.length();

        // Skip spaces and colon
        while (idx < json.length() && (Character.isWhitespace(json.charAt(idx)) || json.charAt(idx) == ':')) {
            idx++;
        }

        if (idx >= json.length()) return null;

        if (json.charAt(idx) == '"') {
            idx++; // skip start quote
            int end = idx;
            while (end < json.length()) {
                if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') {
                    break;
                }
                end++;
            }
            if (end >= json.length()) return "";
            return json.substring(idx, end).replace("\\\"", "\"").replace("\\\\", "\\");
        } else {
            int end = idx;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}' && json.charAt(end) != ']') {
                end++;
            }
            return json.substring(idx, end).trim();
        }
    }


    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
