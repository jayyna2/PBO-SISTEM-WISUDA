package app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class StaticHandler implements HttpHandler {
    private final String baseDir;

    public StaticHandler() {
        String userDir = System.getProperty("user.dir");
        System.out.println("[StaticServer] Current working directory: " + userDir);

        File[] checkDirs = {
            new File("web"),
            new File("../web")
        };

        String foundDir = null;
        for (File dir : checkDirs) {
            System.out.println("[StaticServer] Checking path: " + dir.getAbsolutePath() + " (exists: " + dir.exists() + ")");
            if (dir.exists() && dir.isDirectory()) {
                foundDir = dir.getAbsolutePath();
                break;
            }
        }

        if (foundDir != null) {
            this.baseDir = foundDir;
            System.out.println("[StaticServer] Serving static files from: " + this.baseDir);
        } else {
            this.baseDir = "web";
            System.err.println("[StaticServer] WARNING: Could not locate web directory! Fallback to 'web'");
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (!"GET".equalsIgnoreCase(method)) {
            sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        
        // Prevent directory traversal attacks
        if (path.contains("..")) {
            sendError(exchange, 403, "Access Forbidden");
            return;
        }

        // Default routing to index.html
        if (path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }

        File file = new File(baseDir, path.substring(1));
        if (!file.exists() || file.isDirectory()) {
            sendError(exchange, 404, "File Not Found: " + path);
            return;
        }

        String mime = getMimeType(file.getName());
        exchange.getResponseHeaders().set("Content-Type", mime);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");

        long fileLength = file.length();
        exchange.sendResponseHeaders(200, fileLength);

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = exchange.getResponseBody()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
        }
    }

    private void sendError(HttpExchange exchange, int status, String msg) throws IOException {
        byte[] responseBytes = msg.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(status, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private String getMimeType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=utf-8";
        if (lower.endsWith(".css")) return "text/css; charset=utf-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".json")) return "application/json; charset=utf-8";
        return "application/octet-stream";
    }
}
