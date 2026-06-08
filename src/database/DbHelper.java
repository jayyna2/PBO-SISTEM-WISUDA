package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import model.*;

public class DbHelper {
    private static final String URL = "jdbc:mysql://localhost:3306/db_wisuda?useSSL=false&useUnicode=true&characterEncoding=UTF-8&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // Default Laragon password is empty

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException ex) {
                System.err.println("MySQL Driver not found!");
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // 1. Account Login
    public User login(String usernameOrNim, String password, String role) {
        String sql;
        if ("mahasiswa".equalsIgnoreCase(role)) {
            sql = "SELECT nim, nama FROM tabel_mahasiswa WHERE nim = ? AND password = ?";
        } else {
            sql = "SELECT id, nama_lengkap FROM tabel_admin WHERE username = ? AND password = ?";
        }

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usernameOrNim);
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    if ("mahasiswa".equalsIgnoreCase(role)) {
                        return new Mahasiswa(rs.getString("nim"), rs.getString("nama"), null, null, null, null, null, 0.0);
                    } else {
                        return new Admin(usernameOrNim, null, rs.getString("nama_lengkap"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 2. Get Student Profile
    public Mahasiswa getMahasiswaProfile(String nim) {
        String sql = "SELECT * FROM tabel_mahasiswa WHERE nim = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nim);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Mahasiswa(
                        rs.getString("nim"),
                        rs.getString("nama"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getString("telepon"),
                        rs.getString("fakultas"),
                        rs.getString("prodi"),
                        rs.getDouble("ipk")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 3. Register Graduation Form
    public String registerWisuda(String nim, String periode, String judul) {
        // Check if already registered
        String checkSql = "SELECT id_pendaftaran FROM tabel_pendaftaran_wisuda WHERE nim = ?";
        String insertSql = "INSERT INTO tabel_pendaftaran_wisuda (nim, periode_wisuda, judul_skripsi, tanggal_daftar, status, keterangan) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                stmt.setString(1, nim);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return "{\"success\":false,\"error\":\"Anda sudah mengisi formulir pendaftaran.\"}";
                    }
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setString(1, nim);
                stmt.setString(2, periode);
                stmt.setString(3, judul);
                stmt.setDate(4, new java.sql.Date(System.currentTimeMillis()));
                stmt.setString(5, "PENDING");
                stmt.setString(6, "Pendaftaran masuk. Silakan lengkapi upload dokumen berkas dan bukti pembayaran.");
                
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    return "{\"success\":true,\"message\":\"Formulir pendaftaran berhasil dikirim.\"}";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "{\"success\":false,\"error\":\"Kesalahan database: " + escapeJson(e.getMessage()) + "\"}";
        }
        return "{\"success\":false,\"error\":\"Gagal menyimpan pendaftaran.\"}";
    }

    // 4. Save Uploaded Files Paths
    public boolean saveUploadedFiles(String nim, String fotoPath, String syaratPath, String buktiPath) {
        String checkBerkas = "SELECT id_berkas FROM tabel_berkas WHERE nim = ?";
        String checkBayar = "SELECT id_pembayaran FROM tabel_pembayaran WHERE nim = ?";
        
        try (Connection conn = getConnection()) {
            // Save Berkas (Photo and Requirements)
            boolean berkasExists = false;
            try (PreparedStatement stmt = conn.prepareStatement(checkBerkas)) {
                stmt.setString(1, nim);
                try (ResultSet rs = stmt.executeQuery()) {
                    berkasExists = rs.next();
                }
            }
            
            if (fotoPath != null || syaratPath != null) {
                if (berkasExists) {
                    StringBuilder sb = new StringBuilder("UPDATE tabel_berkas SET status_verifikasi = 'PENDING'");
                    if (fotoPath != null) sb.append(", foto_path = ?");
                    if (syaratPath != null) sb.append(", persyaratan_path = ?");
                    sb.append(" WHERE nim = ?");
                    
                    try (PreparedStatement stmt = conn.prepareStatement(sb.toString())) {
                        int idx = 1;
                        if (fotoPath != null) stmt.setString(idx++, fotoPath);
                        if (syaratPath != null) stmt.setString(idx++, syaratPath);
                        stmt.setString(idx, nim);
                        stmt.executeUpdate();
                    }
                } else {
                    String insert = "INSERT INTO tabel_berkas (nim, foto_path, persyaratan_path, status_verifikasi) VALUES (?, ?, ?, 'PENDING')";
                    try (PreparedStatement stmt = conn.prepareStatement(insert)) {
                        stmt.setString(1, nim);
                        stmt.setString(2, fotoPath);
                        stmt.setString(3, syaratPath);
                        stmt.executeUpdate();
                    }
                }
            }

            // Save Pembayaran
            if (buktiPath != null) {
                boolean payExists = false;
                try (PreparedStatement stmt = conn.prepareStatement(checkBayar)) {
                    stmt.setString(1, nim);
                    try (ResultSet rs = stmt.executeQuery()) {
                        payExists = rs.next();
                    }
                }

                if (payExists) {
                    String update = "UPDATE tabel_pembayaran SET bukti_bayar_path = ?, status_pembayaran = 'PENDING', tanggal_bayar = ? WHERE nim = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(update)) {
                        stmt.setString(1, buktiPath);
                        stmt.setDate(2, new java.sql.Date(System.currentTimeMillis()));
                        stmt.setString(3, nim);
                        stmt.executeUpdate();
                    }
                } else {
                    String insert = "INSERT INTO tabel_pembayaran (nim, jumlah_bayar, bukti_bayar_path, status_pembayaran, tanggal_bayar) VALUES (?, 750000.00, ?, 'PENDING', ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(insert)) {
                        stmt.setString(1, nim);
                        stmt.setString(2, buktiPath);
                        stmt.setDate(3, new java.sql.Date(System.currentTimeMillis()));
                        stmt.executeUpdate();
                    }
                }
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 5. Get Student Status details (as OOP Model)
    public Pendaftaran getPendaftaran(String nim) {
        String regSql = "SELECT id_pendaftaran, status, keterangan, periode_wisuda, judul_skripsi, tanggal_daftar FROM tabel_pendaftaran_wisuda WHERE nim = ?";
        String berkasSql = "SELECT foto_path, persyaratan_path, status_verifikasi FROM tabel_berkas WHERE nim = ?";
        String paySql = "SELECT bukti_bayar_path, status_pembayaran FROM tabel_pembayaran WHERE nim = ?";

        try (Connection conn = getConnection()) {
            Mahasiswa mhs = getMahasiswaProfile(nim);
            if (mhs == null) return null;

            Pendaftaran pend = null;
            try (PreparedStatement stmt = conn.prepareStatement(regSql)) {
                stmt.setString(1, nim);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        pend = new Pendaftaran(
                            rs.getInt("id_pendaftaran"),
                            mhs,
                            rs.getString("periode_wisuda"),
                            rs.getString("judul_skripsi"),
                            rs.getDate("tanggal_daftar") != null ? rs.getDate("tanggal_daftar").toString() : "",
                            rs.getString("status"),
                            rs.getString("keterangan")
                        );
                    }
                }
            }

            if (pend != null) {
                // Fetch Berkas
                try (PreparedStatement stmt = conn.prepareStatement(berkasSql)) {
                    stmt.setString(1, nim);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            Berkas berkas = new Berkas(
                                nim,
                                rs.getString("foto_path"),
                                rs.getString("persyaratan_path"),
                                rs.getString("status_verifikasi")
                            );
                            pend.setBerkas(berkas);
                        }
                    }
                }

                // Fetch Pembayaran
                try (PreparedStatement stmt = conn.prepareStatement(paySql)) {
                    stmt.setString(1, nim);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            Pembayaran pembayaran = new Pembayaran(
                                nim,
                                750000.00,
                                rs.getString("bukti_bayar_path"),
                                rs.getString("status_pembayaran"),
                                null
                            );
                            pend.setPembayaran(pembayaran);
                        }
                    }
                }
            }
            return pend;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 6. Get Admin Statistics Summary
    public String getAdminStats() {
        String sqlTotal = "SELECT COUNT(id_pendaftaran) as total FROM tabel_pendaftaran_wisuda";
        String sqlPending = "SELECT COUNT(id_pendaftaran) as total FROM tabel_pendaftaran_wisuda WHERE status = 'PENDING'";
        String sqlApproved = "SELECT COUNT(id_pendaftaran) as total FROM tabel_pendaftaran_wisuda WHERE status = 'APPROVED'";
        String sqlIpk = "SELECT AVG(m.ipk) as avg_ipk FROM tabel_pendaftaran_wisuda p JOIN tabel_mahasiswa m ON p.nim = m.nim";

        int total = 0, pending = 0, approved = 0;
        double avgIpk = 0.0;

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sqlTotal); ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) total = rs.getInt("total");
            }
            try (PreparedStatement stmt = conn.prepareStatement(sqlPending); ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) pending = rs.getInt("total");
            }
            try (PreparedStatement stmt = conn.prepareStatement(sqlApproved); ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) approved = rs.getInt("total");
            }
            try (PreparedStatement stmt = conn.prepareStatement(sqlIpk); ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) avgIpk = rs.getDouble("avg_ipk");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return String.format(Locale.US,
                "{\"total\":%d,\"pending\":%d,\"approved\":%d,\"avgIpk\":%.2f}",
                total, pending, approved, avgIpk);
    }

    // 7. Get All Registrations for Admin (as OOP Model list)
    public List<Pendaftaran> getAdminPendaftar() {
        String sql = "SELECT p.*, m.nama, m.email, m.telepon, m.fakultas, m.prodi, m.ipk, " +
                     "b.foto_path, b.persyaratan_path, b.status_verifikasi, " +
                     "pay.bukti_bayar_path, pay.status_pembayaran " +
                     "FROM tabel_pendaftaran_wisuda p " +
                     "JOIN tabel_mahasiswa m ON p.nim = m.nim " +
                     "LEFT JOIN tabel_berkas b ON p.nim = b.nim " +
                     "LEFT JOIN tabel_pembayaran pay ON p.nim = pay.nim " +
                     "ORDER BY p.tanggal_daftar DESC";

        List<Pendaftaran> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Mahasiswa m = new Mahasiswa(
                    rs.getString("nim"),
                    rs.getString("nama"),
                    null,
                    rs.getString("email"),
                    rs.getString("telepon"),
                    rs.getString("fakultas"),
                    rs.getString("prodi"),
                    rs.getDouble("ipk")
                );

                Pendaftaran p = new Pendaftaran(
                    rs.getInt("id_pendaftaran"),
                    m,
                    rs.getString("periode_wisuda"),
                    rs.getString("judul_skripsi"),
                    rs.getDate("tanggal_daftar") != null ? rs.getDate("tanggal_daftar").toString() : "",
                    rs.getString("status"),
                    rs.getString("keterangan")
                );

                String fotoPath = rs.getString("foto_path");
                String syaratPath = rs.getString("persyaratan_path");
                String statusBerkas = rs.getString("status_verifikasi");
                if (fotoPath != null || syaratPath != null || statusBerkas != null) {
                    Berkas b = new Berkas(m.getNim(), fotoPath, syaratPath, statusBerkas != null ? statusBerkas : "PENDING");
                    p.setBerkas(b);
                }

                String buktiPath = rs.getString("bukti_bayar_path");
                String statusPembayaran = rs.getString("status_pembayaran");
                if (buktiPath != null || statusPembayaran != null) {
                    Pembayaran pay = new Pembayaran(m.getNim(), 750000.00, buktiPath, statusPembayaran != null ? statusPembayaran : "PENDING", null);
                    p.setPembayaran(pay);
                }

                list.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 8. Verify Files / Payments / Registration Decision
    public String verify(String nim, String type, String status, String remarks) {
        String sql;
        if ("berkas".equalsIgnoreCase(type)) {
            sql = "UPDATE tabel_berkas SET status_verifikasi = ? WHERE nim = ?";
        } else if ("pembayaran".equalsIgnoreCase(type)) {
            sql = "UPDATE tabel_pembayaran SET status_pembayaran = ? WHERE nim = ?";
        } else {
            sql = "UPDATE tabel_pendaftaran_wisuda SET status = ?, keterangan = ? WHERE nim = ?";
        }

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if ("berkas".equalsIgnoreCase(type) || "pembayaran".equalsIgnoreCase(type)) {
                stmt.setString(1, status);
                stmt.setString(2, nim);
            } else {
                stmt.setString(1, status);
                stmt.setString(2, remarks);
                stmt.setString(3, nim);
            }
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                return "{\"success\":true}";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
        return "{\"success\":false,\"error\":\"NIM tidak ditemukan.\"}";
    }

    // 9. Get All Student Accounts (CRUD)
    public List<Mahasiswa> getStudents() {
        String sql = "SELECT m.*, p.status as reg_status FROM tabel_mahasiswa m " +
                     "LEFT JOIN tabel_pendaftaran_wisuda p ON m.nim = p.nim " +
                     "ORDER BY m.nim ASC";

        List<Mahasiswa> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Mahasiswa m = new Mahasiswa(
                    rs.getString("nim"),
                    rs.getString("nama"),
                    rs.getString("password"),
                    rs.getString("email"),
                    rs.getString("telepon"),
                    rs.getString("fakultas"),
                    rs.getString("prodi"),
                    rs.getDouble("ipk")
                );
                m.setRegStatus(rs.getString("reg_status") != null ? rs.getString("reg_status") : "BELUM_DAFTAR");
                list.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 10. Add Student Profile
    public String addStudent(String nim, String nama, String pwd, String email, String telp, String fak, String prodi, double ipk) {
        String checkSql = "SELECT nim FROM tabel_mahasiswa WHERE nim = ?";
        String insertSql = "INSERT INTO tabel_mahasiswa (nim, nama, password, email, telepon, fakultas, prodi, ipk) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                stmt.setString(1, nim);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) return "{\"success\":false,\"error\":\"NIM sudah terdaftar.\"}";
                }
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setString(1, nim);
                stmt.setString(2, nama);
                stmt.setString(3, pwd);
                stmt.setString(4, email);
                stmt.setString(5, telp);
                stmt.setString(6, fak);
                stmt.setString(7, prodi);
                stmt.setDouble(8, ipk);
                
                int rows = stmt.executeUpdate();
                if (rows > 0) return "{\"success\":true}";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
        return "{\"success\":false,\"error\":\"Gagal menambah mahasiswa.\"}";
    }

    // 11. Update Student Profile
    public String updateStudent(String nim, String nama, String pwd, String email, String telp, String fak, String prodi, double ipk) {
        StringBuilder sql = new StringBuilder("UPDATE tabel_mahasiswa SET nama = ?, email = ?, telepon = ?, fakultas = ?, prodi = ?, ipk = ?");
        boolean updatePwd = pwd != null && !pwd.isEmpty();
        if (updatePwd) {
            sql.append(", password = ?");
        }
        sql.append(" WHERE nim = ?");

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            stmt.setString(idx++, nama);
            stmt.setString(idx++, email);
            stmt.setString(idx++, telp);
            stmt.setString(idx++, fak);
            stmt.setString(idx++, prodi);
            stmt.setDouble(idx++, ipk);
            if (updatePwd) {
                stmt.setString(idx++, pwd);
            }
            stmt.setString(idx, nim);
            
            int rows = stmt.executeUpdate();
            if (rows > 0) return "{\"success\":true}";
        } catch (SQLException e) {
            e.printStackTrace();
            return "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
        return "{\"success\":false,\"error\":\"NIM tidak ditemukan.\"}";
    }

    // 12. Delete Student Profile
    public String deleteStudent(String nim) {
        String sql = "DELETE FROM tabel_mahasiswa WHERE nim = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nim);
            int rows = stmt.executeUpdate();
            if (rows > 0) return "{\"success\":true}";
        } catch (SQLException e) {
            e.printStackTrace();
            return "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
        return "{\"success\":false,\"error\":\"NIM tidak ditemukan.\"}";
    }

    // JSON Escaper Helper
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
