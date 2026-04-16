import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

// Lightweight HTTP API server for MiniCompiler
public class CompilerServer {

    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    // Session-level symbol table shared across requests
    private static final Map<String, Double> sessionSymbols =
            Collections.synchronizedMap(new LinkedHashMap<>());

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/compile",    new CompileHandler());
        server.createContext("/vars",       new VarsHandler());
        server.createContext("/vars/clear", new ClearHandler());
        server.createContext("/health",     new HealthHandler());
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("[CompilerServer] Listening on http://localhost:" + PORT);
        System.out.println("[CompilerServer] Press Ctrl+C to stop.");
    }

    // /compile
    static class CompileHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            addCors(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
            if (!"POST".equals(ex.getRequestMethod())) { respond(ex, 405, "{\"error\":\"Method not allowed\"}"); return; }

            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String expression = extractJson(body, "expression");
            if (expression == null || expression.isBlank()) {
                respond(ex, 400, "{\"error\":\"Missing 'expression' field\"}");
                return;
            }

            try {
                MiniCompiler.CompileResult cr = MiniCompiler.compile(expression, sessionSymbols);
                respond(ex, 200, cr.toJson());
            } catch (Exception e) {
                respond(ex, 500, "{\"error\":\"Internal error: " + MiniCompiler.esc(e.getMessage()) + "\"}");
            }
        }
    }

    // /vars
    static class VarsHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            addCors(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Double> e : sessionSymbols.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(MiniCompiler.esc(e.getKey())).append("\":")
                  .append(MiniCompiler.fmt(e.getValue()));
                first = false;
            }
            sb.append("}");
            respond(ex, 200, sb.toString());
        }
    }

    // /vars/clear
    static class ClearHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            addCors(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
            sessionSymbols.clear();
            respond(ex, 200, "{\"ok\":true}");
        }
    }

    // /health
    static class HealthHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            addCors(ex);
            respond(ex, 200, "{\"status\":\"ok\",\"version\":\"2.0\"}");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────
    static void respond(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    static void addCors(HttpExchange ex) {
        Headers h = ex.getResponseHeaders();
        h.set("Access-Control-Allow-Origin",  "*");
        h.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        h.set("Access-Control-Allow-Headers", "Content-Type");
    }

    // Minimal JSON string extractor
    static String extractJson(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        idx = json.indexOf(":", idx + search.length());
        if (idx == -1) return null;
        idx++;
        while (idx < json.length() && Character.isWhitespace(json.charAt(idx))) idx++;
        if (idx >= json.length() || json.charAt(idx) != '"') return null;
        idx++; // skip opening quote
        StringBuilder sb = new StringBuilder();
        while (idx < json.length()) {
            char c = json.charAt(idx++);
            if (c == '\\' && idx < json.length()) {
                char next = json.charAt(idx++);
                switch (next) {
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n'  -> sb.append('\n');
                    case 'r'  -> sb.append('\r');
                    case 't'  -> sb.append('\t');
                    default   -> sb.append(next);
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
