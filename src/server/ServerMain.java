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
        server.setExecutor(Executors.newFixedThreadPool(8)); // Thread pool for concurrency
        server.start();
        System.out.println("✅ Server started on http://localhost:8080/api");
    }

    private static void handleApi(HttpExchange ex) throws IOException {
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

            response = switch (req.action) {
                case "login" -> handleLogin(req);
                case "getCredits" -> handleGetCredits(req);
                case "chargeCredits" -> handleChargeCredits(req);
                case "getUsers" -> handleGetUsers(req);
                case "userStatistics" -> handleUserStatistics(req);
                case "logout" -> handleLogout(req);
                case "uploadFile" -> handleUploadFile(req);
                case "ping" -> new BaseResponse(true, "pong");
                case "getPrograms" -> handlePrograms(req);
                case "getFunctions" -> handleFunctions(req);
                case "setProgramToUser" -> handleExecuteProgram(req);
                case "getProgramInstructions" -> handleGetInstructions(req);
                case "getHistoryInstruction" -> handleGetInstructionHistory(req);
                default -> new BaseResponse(false, "Unknown action: " + req.action);
            };

            sendResponse(ex, 200, response);

        } catch (Exception e) {
            e.printStackTrace();
            sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }

    // ---------------------- HANDLERS ----------------------

    private static BaseResponse handleLogin(BaseRequest req) {
        String username = getString(req, "username");

        if (!validateParameter(username)) {
            return new BaseResponse(false, "Invalid username");
        }

        int result = EngineManager.getInstance().addUser(username);

        return switch (result) {
            case ERROR_CODES.ERROR_USER_EXISTS -> new BaseResponse(false, "User already logged in");
            case ERROR_CODES.ERROR_OK -> new BaseResponse(true, "Welcome " + username);
            default -> new BaseResponse(false, "Server error");
        };
    }

    private static BaseResponse handleGetCredits(BaseRequest req) {
        String username = getString(req, "username");

        if (!validateParameter(username)) {
            return new BaseResponse(false, "Invalid username");
        }

        int credits = EngineManager.getInstance().getCredit(username);
        if (credits == ERROR_CODES.ERROR_USER_EXISTS)
            return new BaseResponse(false, "User not logged in");
        if (credits < 0)
            return new BaseResponse(false, "Server error");

        return new BaseResponse(true, "Credits loaded").add("credits", credits);

    }

    private static BaseResponse handleChargeCredits(BaseRequest req) {
        String username = getString(req, "username");
        Integer amount = getInt(req, "amount");
        if (!validateParameter(username)) {
            return new BaseResponse(false, "Invalid username");
        }
        if (!validateParameter(amount)) {
            return new BaseResponse(false, "Invalid credits amount");
        }

        int newBalance = EngineManager.getInstance().chargeCredits(username, amount);

        if (newBalance == ERROR_CODES.ERROR_USER_NOT_FOUND)
            return new BaseResponse(false, "User not logged in");
        if (newBalance == ERROR_CODES.ERROR_INVALID_CREDENTIALS)
            return new BaseResponse(false, "Invalid amount");
        if (newBalance < 0)
            return new BaseResponse(false, "Server error");

        return new BaseResponse(true, "Credits charged successfully").add("newBalance", newBalance);

    }

    private static BaseResponse handleGetUsers(BaseRequest req) {
        List<Map<String, Object>> usersList = new ArrayList<>();
        int result = EngineManager.getInstance().fetchUsers(usersList);
        if (result != ERROR_CODES.ERROR_OK)
            return new BaseResponse(false, "Failed to fetch users");

        return new BaseResponse(true, "Users fetched successfully").add("users", usersList);
    }

    private static BaseResponse handleUserStatistics(BaseRequest req) {
        String username = getString(req, "username");
        if (!validateParameter(username)) {
            return new BaseResponse(false, "Invalid username");
        }

        Map<String, Object> statistics = new LinkedHashMap<>();
        int result = EngineManager.getInstance().fetchUserStatistic(username, statistics);

        return switch (result) {
            case ERROR_CODES.ERROR_USER_NOT_FOUND -> new BaseResponse(false, "User not logged in");
            case ERROR_CODES.ERROR_OK -> new BaseResponse(true, "Statistics collected").add("statistics", statistics);
            default -> new BaseResponse(false, "Failed to fetch statistics");
        };
    }

    private static BaseResponse handleLogout(BaseRequest req) {
        String username = getString(req, "username");
        if (!validateParameter(username)) {
            return new BaseResponse(false, "Invalid username");
        }

        int result = EngineManager.getInstance().logout(username);
        return switch (result) {
            case ERROR_CODES.ERROR_USER_NOT_FOUND -> new BaseResponse(false, "User not logged in");
            case ERROR_CODES.ERROR_OK -> new BaseResponse(true, "Logged out successfully");
            default -> new BaseResponse(false, "Failed to logout");
        };
    }

    private static BaseResponse handleUploadFile(BaseRequest req) {
        String username = getString(req, "username");
        String filename = getString(req, "filename");
        String base64Data = getString(req, "fileData");

        if (username == null || filename == null || base64Data == null)
            return new BaseResponse(false, "Missing upload data");

        int result = EngineManager.getInstance().addProgram(username, filename, base64Data);

        return switch (result) {
            case ERROR_CODES.ERROR_INVALID_FILE -> new BaseResponse(false, "Invalid XML format");
            case ERROR_CODES.ERROR_PROGRAM_EXISTS -> new BaseResponse(false, "Program already exists");
            case ERROR_CODES.ERROR_FUNCTION_MISSING -> new BaseResponse(false, "Program requires a missing function");
            case ERROR_CODES.ERROR_OK -> new BaseResponse(true, "File uploaded successfully");
            default -> new BaseResponse(false, "Failed to upload program");
        };
    }

    private static BaseResponse handlePrograms(BaseRequest req) {
        List<Map<String, Object>> programList = new ArrayList<>();
        int result = EngineManager.getInstance().fetchPrograms(programList);
        if (result != ERROR_CODES.ERROR_OK)
            return new BaseResponse(false, "Failed to fetch programs");

        return new BaseResponse(true, "Programs fetched successfully").add("programs", programList);
    }

    private static BaseResponse handleFunctions(BaseRequest req) {
        List<Map<String, Object>> functionsList = new ArrayList<>();
        int result = EngineManager.getInstance().fetchFunctions(functionsList);
        if (result != ERROR_CODES.ERROR_OK)
            return new BaseResponse(false, "Failed to fetch functions");

        return new BaseResponse(true, "Functions fetched successfully").add("functions", functionsList);
    }

    private static BaseResponse handleExecuteProgram(BaseRequest req) {
        String username = getString(req, "username");
        String programName = getString(req, "programName");

        if (!validateParameter(username)) {
            return new BaseResponse(false, "Invalid username");
        }

        if (!validateParameter(programName)) {
            return new BaseResponse(false, "Invalid programName");
        }

        int result = EngineManager.getInstance().setProgramToUser(username, programName);
        return switch (result) {
            case ERROR_CODES.ERROR_PROGRAM_NOT_FOUND -> new BaseResponse(false, "Program not found");
            case ERROR_CODES.ERROR_USER_NOT_FOUND -> new BaseResponse(false, "User not found");
            case ERROR_CODES.ERROR_OK -> new BaseResponse(true, "Program set successfully");
            default -> new BaseResponse(false, "Server error");
        };
    }

    private static BaseResponse handleGetInstructions(BaseRequest req) {
        String username = getString(req, "username");
        if (!validateParameter(username)) {
            return new BaseResponse(false, "Invalid username");
        }

        List<Map<String, Object>> instructions = new ArrayList<>();
        int result = EngineManager.getInstance().getProgramInstructions(username, instructions);

        return switch (result) {
            case ERROR_CODES.ERROR_USER_NOT_FOUND -> new BaseResponse(false, "User not found");
            case ERROR_CODES.ERROR_PROGRAM_NOT_FOUND -> new BaseResponse(false, "No program set for user");
            case ERROR_CODES.ERROR_OK -> new BaseResponse(true, "Instructions fetched")
                    .add("instructions", instructions);
            default -> new BaseResponse(false, "Server error");
        };
    }

    private static BaseResponse handleGetInstructionHistory(BaseRequest req) {
        String username = getString(req, "username");
        Integer instructionNumber = getInt(req, "instructionNumber");

        if (!validateParameter(username)) {
            return new BaseResponse(false, "Invalid username");
        }
        if (!validateParameter(instructionNumber)) {
            return new BaseResponse(false, "Invalid instructionNumber");
        }

        List<Map<String, Object>> history = new ArrayList<>();
        int result = EngineManager.getInstance().getInstructionHistory(username, instructionNumber, history);

        return switch (result) {
            case ERROR_CODES.ERROR_USER_NOT_FOUND -> new BaseResponse(false, "User not found");
            case ERROR_CODES.ERROR_PROGRAM_NOT_FOUND -> new BaseResponse(false, "No program set for user");
            case ERROR_CODES.ERROR_INVALID_INSTRUCTION_NUMBER -> new BaseResponse(false, "Invalid instruction number");
            case ERROR_CODES.ERROR_OK -> new BaseResponse(true, "History fetched")
                    .add("historyInstruction", history);
            default -> new BaseResponse(false, "Server error");
        };
    }

    // ---------------------- UTILITIES ----------------------
    private static boolean validateParameter(String param) {
        return (param != null && !param.isBlank());
    }

    private static boolean validateParameter(Integer param) {
        return (param != null);
    }

    private static String getString(BaseRequest req, String key) {
        Object val = req.data.get(key);
        return val instanceof String ? (String) val : null;
    }

    private static Integer getInt(BaseRequest req, String key) {
        Object val = req.data.get(key);
        return val instanceof Number ? ((Number) val).intValue() : null;
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
