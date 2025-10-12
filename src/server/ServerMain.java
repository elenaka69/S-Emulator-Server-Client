package server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import server.engine.execution.ERROR_CODES;
import server.engine.execution.EngineManager;
import shared.BaseRequest;
import shared.BaseResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;

public class ServerMain {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8080), 0);
        server.createContext("/api", ServerMain::handleApi);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("✅ Server started on http://localhost:8080/api");
    }

    private static void handleApi(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(405, -1);
            return;
        }

        try {
            BaseRequest req = mapper.readValue(ex.getRequestBody(), BaseRequest.class);
          //  System.out.println("📩 Received: " + req);

            BaseResponse resp = switch (req.action) {
                case "login" -> handleLogin(req);
                case "getCredits" -> handleGetCredits(req);
                case "chargeCredits" -> handleChargeCredits(req);
                case "getUsers" -> handleGetUsers(req);
                case "userStatistics" -> handleUserStatistics(req);
                case "logout" ->handleLogout(req);
                case "uploadFile" -> handleUploadFile(req);
                case "ping" -> new BaseResponse(true, "pong");
                case "getPrograms" -> handlePrograms(req);
                case "getFunctions" -> handleFunctions(req);
                case "setProgramToUser" -> handleExecuteProgram(req);
                case "getProgramInstructions" -> handleGetInstructions(req);
                case "getHistoryInstruction" -> handleGetInstructionHistory(req);
                default -> new BaseResponse(false, "Unknown action: " + req.action);
            };

            byte[] json = mapper.writeValueAsBytes(resp);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, json.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(json);
            }

        } catch (Exception e) {
            e.printStackTrace();
            BaseResponse err = new BaseResponse(false, "Server error: " + e.getMessage());
            byte[] json = mapper.writeValueAsBytes(err);
            ex.sendResponseHeaders(500, json.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(json);
            }
        }
    }

    private static BaseResponse handleLogin(BaseRequest req) {
        String username = (String) req.data.get("username");
        if (username == null || username.isBlank()) {
            return new BaseResponse(false, "Username is required");
        }
        int result = EngineManager.addUser(username);

        if (result == ERROR_CODES.ERROR_USER_EXISTS)
            return new BaseResponse(false, "User already logged in");

        return new BaseResponse(true, "Welcome " + username);
    }

    private static BaseResponse handleGetCredits(BaseRequest req) {
        String username = (String) req.data.get("username");

        int credits = EngineManager.getCreadit(username);
        if (credits == ERROR_CODES.ERROR_USER_NOT_FOUND) {
            return new BaseResponse(false, "User not logged in");
        }

        return new BaseResponse(true, "Credits loaded").add("credits", credits);
    }

    private static BaseResponse handleChargeCredits(BaseRequest req) {
        String username = (String) req.data.get("username");
        Integer amount = (Integer) req.data.get("amount");

        int newBalance = EngineManager.ChargeCredits(username, amount);
        if (newBalance == ERROR_CODES.ERROR_USER_NOT_FOUND)
            return new BaseResponse(false, "User not logged in");
        if (newBalance == ERROR_CODES.ERROR_INVALID_CREDENTIALS)
            return new BaseResponse(false, "Invalid amount");

        return new BaseResponse(true, "Credits charged successfully").add("newBalance", newBalance);
    }

    private static BaseResponse handleGetUsers(BaseRequest req) {
        List<Map<String, Object>> usersList = new ArrayList<>();
        int result = EngineManager.fetchUsers(usersList);
        if (result != ERROR_CODES.ERROR_OK) {
            return new BaseResponse(false, "Failed to fetch users");
        }
        return new BaseResponse(true, "Users fetched successfully").add("users", usersList);
    }

    private static BaseResponse handleUserStatistics (BaseRequest req) {
        String username = (String) req.data.get("username");
        Map<String, Object> statistics  = new LinkedHashMap<>();

        int result = EngineManager.fetchUserStatistic(username, statistics);
        if (result == ERROR_CODES.ERROR_USER_NOT_FOUND) {
            return new BaseResponse(false, "User not logged in");
        }
        if (result != ERROR_CODES.ERROR_OK)
            return new BaseResponse(false, "Failed to fetch statistics");

        return new BaseResponse(true, "Collected statistic successfully").add("statistics", statistics);
    }

    private static BaseResponse handleLogout(BaseRequest req) {
        String username = (String) req.data.get("username");

        int result = EngineManager.logout(username);
        if (result == ERROR_CODES.ERROR_USER_NOT_FOUND)
            return new BaseResponse(false, "User not logged in");
        if (result != ERROR_CODES.ERROR_OK)
            return new BaseResponse(false, "Failed to logout");

        return new BaseResponse(true, "Logged out successfully");
    }

    private static BaseResponse handleUploadFile(BaseRequest req) {
            String username = (String) req.data.get("username");
            String filename = (String) req.data.get("filename");
            String base64Data = (String) req.data.get("fileData");

            if (username == null || filename == null || base64Data == null)
                return new BaseResponse(false, "Missing upload data");

            int result = EngineManager.addProgram(username, filename, base64Data);

            if (result == ERROR_CODES.ERROR_INVALID_FILE)
                return new BaseResponse(false, "Failed to upload file. Invalid XML format.");
            if (result == ERROR_CODES.ERROR_PROGRAM_EXISTS)
                return new BaseResponse(false, "Failed to upload file. Program already exists.");
            if (result == ERROR_CODES.ERROR_FUNCTION_MISSING)
                return new BaseResponse(false, "Failed to upload file. The program requires a function that is missing.");
            if( result != ERROR_CODES.ERROR_OK)
                return new BaseResponse(false, "Failed to upload programs");

            return new BaseResponse(true, "File uploaded and parsed successfully");
    }

    private static BaseResponse handlePrograms(BaseRequest req) {
        List<Map<String, Object>> programList = new ArrayList<>();

        int result = EngineManager.fetchPrograms(programList);
        if( result != ERROR_CODES.ERROR_OK) {
            return new BaseResponse(false, "Failed to fetch programs");
        }
        return new BaseResponse(true, "Programs fetched successfully").add("programs", programList);
    }

    private static BaseResponse handleFunctions(BaseRequest req) {
        List<Map<String, Object>> functionsList = new ArrayList<>();

        int result = EngineManager.fetchFunctions(functionsList);
        if (result != ERROR_CODES.ERROR_OK) {
            return new BaseResponse(false, "Failed to fetch functions");
        }

        return new BaseResponse(true, "Functions fetched successfully").add("functions", functionsList);
    }

    private static BaseResponse handleExecuteProgram(BaseRequest req) {
        String username = (String) req.data.get("username");
        String programName = (String) req.data.get("programName");

        int result = EngineManager.setProgramToUser(username, programName);
        if (result == ERROR_CODES.ERROR_PROGRAM_NOT_FOUND) {
            return new BaseResponse(false, "Program not found");
        }
        if (result == ERROR_CODES.ERROR_USER_NOT_FOUND) {
            return new BaseResponse(false, "User not found");
        }
        if (result == ERROR_CODES.ERROR_OK) {
            return new BaseResponse(true, "Program " + programName + " set successfully");
        }
        return new BaseResponse(false, "Server error");
    }

    private static BaseResponse handleGetInstructions(BaseRequest req) {
        String username = (String) req.data.get("username");
        List<Map<String, Object>> instructions = new ArrayList<>();
        int result = EngineManager.getProgramInstructions(username, instructions);
        if (result == ERROR_CODES.ERROR_USER_NOT_FOUND) {
            return new BaseResponse(false, "User not found");
        }
        if (result == ERROR_CODES.ERROR_PROGRAM_NOT_FOUND) {
            return new BaseResponse(false, "No program set for user");
        }
        if (result == ERROR_CODES.ERROR_OK) {
            return new BaseResponse(true, "Instructions fetched successfully").add("instructions", instructions);
        }
        return new BaseResponse(false, "Server error");
    }

    private static BaseResponse handleGetInstructionHistory(BaseRequest req) {
        String username = (String) req.data.get("username");
        Integer instructionNumber = (Integer) req.data.get("instructionNumber");
        List<Map<String, Object>> instructions = new ArrayList<>();

        int result = EngineManager.getInstructionHistory(username, instructionNumber, instructions);

        if (result == ERROR_CODES.ERROR_USER_NOT_FOUND) {
            return new BaseResponse(false, "User not found");
        }
        if (result == ERROR_CODES.ERROR_PROGRAM_NOT_FOUND) {
            return new BaseResponse(false, "No program set for user");
        }
        if (result == ERROR_CODES.ERROR_INVALID_INSTRUCTION_NUMBER) {
            return new BaseResponse(false, "Invalid instruction number");
        }
        if (result == ERROR_CODES.ERROR_OK) {
            return new BaseResponse(true, "Instruction history fetched successfully").add("historyInstruction", instructions);
        }
        return new BaseResponse(false, "Server error");
    }
}
