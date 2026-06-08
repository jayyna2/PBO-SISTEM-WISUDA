package model;

import database.DbHelper;
import com.sun.net.httpserver.HttpServer;
import app.ApiHandler;
import app.StaticHandler;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class WisudaApp {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        try {
            // Verify MySQL database connection on startup
            try (java.sql.Connection conn = DbHelper.getConnection()) {
                System.out.println("[DB] Koneksi ke MySQL Laragon berhasil terhubung.");
            } catch (Exception e) {
                System.err.println("[WARNING] Gagal terhubung ke MySQL database (Port 3306).");
                System.err.println("          Pastikan Laragon sudah menyalakan MySQL (Start All)!");
                System.err.println("          Error: " + e.getMessage());
            }

            // Start standalone HttpServer
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            
            // Register Contexts
            server.createContext("/api", new ApiHandler());
            server.createContext("/", new StaticHandler());

            // Use multi-threaded executor for handling parallel requests
            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();

            System.out.println("=================================================================");
            System.out.println("     SISTEM PENDAFTARAN WISUDA - STANDALONE JAVA WEB SERVER      ");
            System.out.println("=================================================================");
            System.out.println("Server berjalan aktif di: http://localhost:" + PORT);
            System.out.println("Tekan CTRL + C untuk mematikan server.");
            System.out.println("=================================================================");

        } catch (Exception e) {
            System.err.println("Gagal memulai server wisuda: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
