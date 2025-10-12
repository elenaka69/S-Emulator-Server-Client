package server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import server.auth.UserManager;
import server.auth.UserProfile;
import server.engine.execution.ERROR_CODES;
import server.engine.execution.EngineManager;
import server.engine.execution.ProgramCollection;
import server.engine.program.FunctionExecutorImpl;
import server.engine.program.SprogramImpl;
import shared.BaseRequest;
import shared.BaseResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.format.DateTimeFormatter;
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
            System.out.println("📩 Received: " + req);

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
                case "executeProgram" -> handleExecuteProgram(req);
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
        if (UserManager.isUserActive(username)) {
            return new BaseResponse(false, "User already logged in");
        }

        UserManager.addUser(username);
        return new BaseResponse(true, "Welcome " + username);
    }

    private static BaseResponse handleGetCredits(BaseRequest req) {
        String username = (String) req.data.get("username");
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null || !profile.isActive()) {
            return new BaseResponse(false, "User not logged in");
        }
        int credits = profile.getCredit();
        return new BaseResponse(true, "Credits loaded").add("credits", credits);
    }

    private static BaseResponse handleChargeCredits(BaseRequest req) {
        String username = (String) req.data.get("username");
        Integer amount = (Integer) req.data.get("amount");

        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null || !profile.isActive()) {
            return new BaseResponse(false, "User not logged in");
        }

        if (amount == null || amount <= 0) {
            return new BaseResponse(false, "Invalid amount");
        }

        int newBalance = profile.getCredit() + amount;
        profile.setCredit(newBalance);
        return new BaseResponse(true, "Credits charged successfully").add("newBalance", newBalance);
    }

    private static BaseResponse handleGetUsers(BaseRequest req) {
        List<Map<String, Object>> usersList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        int num = 1;
        for (Map.Entry<String, UserProfile> entry : UserManager.getActiveUsers().entrySet()) {
            UserProfile profile = entry.getValue();
            if (!profile.isActive()) continue;
            Map<String, Object> row = new HashMap<>();
            row.put("number", num++);
            row.put("userName", entry.getKey());
            row.put("loginTime", profile.getLoginTime().format(formatter));
            usersList.add(row);
        }

        return new BaseResponse(true, "Users fetched successfully").add("users", usersList);
    }

    private static BaseResponse handleUserStatistics (BaseRequest req) {
        String username = (String) req.data.get("username");

        Map<String, Object> statistics  = new LinkedHashMap<>();
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null || !profile.isActive()) {
            return new BaseResponse(false, "User not logged in");
        }
        statistics .put("UserName", username);
        statistics .put("Number of Uploaded Programs", String.valueOf(profile.getNumberPrograms()));
        statistics .put("Number of Uploaded Functions", String.valueOf(profile.getNumberFunctions()));
        statistics .put("Current Credit Balance", String.valueOf(profile.getCredit()));
        statistics .put("Total Spent Credits", String.valueOf(profile.getTotalSpentCredits()));
        statistics .put("Total number of Executions", String.valueOf(profile.getNumberExecutions()));

        return new BaseResponse(true, "Collected statistic successfully").add("statistics", statistics);
    }

    private static BaseResponse handleLogout(BaseRequest req) {
        String username = (String) req.data.get("username");
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null || !profile.isActive()) {
            return new BaseResponse(false, "User not logged in");
        }
        UserManager.logoutUser(username);
        return new BaseResponse(true, "Logged out successfully");
    }

    private static BaseResponse handleUploadFile(BaseRequest req) {
        try {
            String username = (String) req.data.get("username");
            String filename = (String) req.data.get("filename");
            String base64Data = (String) req.data.get("fileData");

            if (username == null || filename == null || base64Data == null) {
                return new BaseResponse(false, "Missing upload data");
            }

            UserProfile profile = UserManager.getActiveUsers().get(username);
            if (profile == null || !profile.isActive()) {
                return new BaseResponse(false, "User not logged in");
            }

            // Decode Base64 → bytes → InputStream
            byte[] fileBytes = Base64.getDecoder().decode(base64Data);
            try (InputStream xmlStream = new ByteArrayInputStream(fileBytes)) {
                // Call your parsing function
                int result = EngineManager.addProgram(username, filename, xmlStream);
                if (result == ERROR_CODES.ERROR_INVALID_FILE)
                    return new BaseResponse(false, "Failed to upload file. Invalid XML format.");
                if (result == ERROR_CODES.ERROR_PROGRAM_EXISTS)
                    return new BaseResponse(false, "Failed to upload file. Program already exists.");
                if (result == ERROR_CODES.ERROR_FUNCTION_MISSING)
                    return new BaseResponse(false, "Failed to upload file. The program requires a function that is missing.");

                return new BaseResponse(true, "File uploaded and parsed successfully");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new BaseResponse(false, "Server error: " + e.getMessage());
        }
    }

    private static BaseResponse handlePrograms(BaseRequest req) {
        List<Map<String, Object>> programList = new ArrayList<>();
        List<String> programs = ProgramCollection.getListPrograms();
        int num = 1;
        for (String programName : programs) {
            SprogramImpl sprogram = ProgramCollection.getProgram(programName);
            Map<String, Object> row = new HashMap<>();
            row.put("number", num++);
            row.put("programName", programName);
            row.put("cost", sprogram.getCost());
            programList.add(row);
        }
        return new BaseResponse(true, "Programs fetched successfully").add("programs", programList);
    }

    private static BaseResponse handleFunctions(BaseRequest req) {
        List<Map<String, Object>> functionsList = new ArrayList<>();
        List<String> functions = ProgramCollection.getListFunctions();
        int num = 1;
        for (String functionName : functions) {
            FunctionExecutorImpl func = ProgramCollection.getFunction(functionName);
            Map<String, Object> row = new HashMap<>();
            row.put("number", num++);
            row.put("functionName", functionName);
            row.put("cost", func.getCost());
            functionsList.add(row);
        }
        return new BaseResponse(true, "Functions fetched successfully").add("functions", functionsList);
    }

    private static BaseResponse handleExecuteProgram(BaseRequest req) {
        String username = (String) req.data.get("username");
        String programName = (String) req.data.get("programName");
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null || !profile.isActive()) {
            return new BaseResponse(false, "User not logged in");

        }
        return new BaseResponse(true, "User not logged in");
    }
}
