package server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import shared.RunResultProperty;
import server.engine.execution.ERROR_CODES;
import server.engine.execution.EngineManager;
import shared.BaseRequest;
import shared.BaseResponse;
import shared.ExecutionStep;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;

public class ServerMain extends ServerApiHandlers {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        new ServerMain().startServer();
    }

    private void startServer() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8080), 0);
        server.createContext("/api", this::handleApi);
        server.createContext("/", this::handleWebDefault);
        server.setExecutor(Executors.newFixedThreadPool(8)); // Thread pool for concurrency
        server.start();
        System.out.println(" Server started at: http://localhost:8080");
        System.out.println(" API available at: http://localhost:8080/api");
    }

    private  void handleWebDefault(HttpExchange exchange) throws IOException {
        addCORS(exchange);

        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";

        // Path relative to resources/webclient
        String resourcePath = "webclient" + path;

        // Load from resources (works both in IntelliJ and JAR)
        InputStream fileStream = ServerMain.class.getClassLoader().getResourceAsStream(resourcePath);
        if (fileStream == null) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        String contentType = getContentType(path);
        exchange.getResponseHeaders().set("Content-Type", contentType);

        byte[] bytes = fileStream.readAllBytes();
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String getContentType(String path) {
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".json")) return "application/json";
        return "text/html";
    }

    private void addCORS(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    private void handleApi(HttpExchange ex) throws IOException {

        addCORS(ex);

        //  Handle preflight requests from browsers
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1); // 204 No Content
            return;
        }

        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(405, -1);
            return;
        }

        BaseResponse response;
        try {
            BaseRequest req = mapper.readValue(ex.getRequestBody(), BaseRequest.class);
            if (req == null || req.action == null) {
                sendError(ex, 400, "Invalid request");
                return;
            }

            response = this.handleApi(req);

            sendResponse(ex, 200, response);

        } catch (Exception e) {
            e.printStackTrace();
            sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }

    private static void sendResponse(HttpExchange ex, int status, BaseResponse resp) throws IOException {
        byte[] json = mapper.writeValueAsBytes(resp);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, json.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(json);
        }
    }

    private static void sendError(HttpExchange ex, int code, String message) throws IOException {
        sendResponse(ex, code, new BaseResponse(false, message));
    }

}
