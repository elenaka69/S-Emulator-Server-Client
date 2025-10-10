package server;

import com.fasterxml.jackson.databind.ObjectMapper;
import server.auth.UserManager;
import shared.LoginRequest;
import shared.LoginResponse;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class ServerMain {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8080), 0);
        server.createContext("/login", ServerMain::handleLogin);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("Server started on http://localhost:8080");
    }

    private static void handleLogin(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(405, -1);
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        LoginRequest req = mapper.readValue(ex.getRequestBody(), LoginRequest.class);
        if (UserManager.isUserActive(req.username))
        {
            LoginResponse resp = new LoginResponse(false, "User already logged in");
            byte[] data = mapper.writeValueAsBytes(resp);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, data.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(data); }
            return;
        }
        UserManager.addUser(req.username);

        LoginResponse resp = new LoginResponse(true, "Welcome!   " + req.username);
        byte[] data = mapper.writeValueAsBytes(resp);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(200, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }
}
