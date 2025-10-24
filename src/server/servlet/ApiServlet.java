package server.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.ServerApiHandlers;
import shared.BaseRequest;
import shared.BaseResponse;

import java.io.IOException;


@WebServlet("/api") // maps to /api
public class ApiServlet extends HttpServlet {

    private final ServerApiHandlers handlers = new ServerApiHandlers(); // composition
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Handle preflight CORS requests
        addCORS(resp);
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        addCORS(resp);

        try {
            BaseRequest request = mapper.readValue(req.getInputStream(), BaseRequest.class);
            if (request == null || request.action == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
                return;
            }

            BaseResponse response = handlers.handleApi(request);
            sendResponse(resp, HttpServletResponse.SC_OK, response);

        } catch (Exception e) {
            e.printStackTrace();
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    private void addCORS(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    private void sendResponse(HttpServletResponse resp, int status, BaseResponse response) throws IOException {
        resp.setContentType("application/json");
        resp.setStatus(status);
        resp.getWriter().write(mapper.writeValueAsString(response));
    }

    private void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        sendResponse(resp, status, new BaseResponse(false, message));
    }
}

