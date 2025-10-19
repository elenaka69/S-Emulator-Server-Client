package server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import server.auth.RunResultProperty;
import server.engine.execution.ERROR_CODES;
import server.engine.execution.EngineManager;
import shared.BaseRequest;
import shared.BaseResponse;
import shared.ExecutionStep;

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
                /* Login Page */
                case "login" -> handleLogin(req);
                case "logout" -> handleLogout(req);
                case "removeUser" -> handleRemoveUser(req);
                /* DashBoard page */
                case "getCredits" -> handleGetCredits(req);
                case "chargeCredits" -> handleChargeCredits(req);
                case "getUsers" -> handleGetUsers(req);
                case "userStatistics" -> handleUserStatistics(req);
                case "uploadFile" -> handleUploadFile(req);
                case "ping" -> new BaseResponse(true, "pong");
                case "getPrograms" -> handlePrograms(req);
                case "getFunctions" -> handleFunctions(req);
                case "sendMessage" -> handleSendMessage(req);
                case "getMessages" -> handleGendMessage(req);
                /* Execution Page */
                case "setProgramToUser" -> handleExecuteProgram(req);
                case "getProgramInstructions" -> handleGetInstructions(req);
                case "getHistoryInstruction" -> handleGetInstructionHistory(req);
                case "getProgramFunctions" -> handleGetProgramFunctions(req);
                case "setWokFunctionUser" -> handleSetWokFunctionUser(req);
                case "getHighlightOptions" -> handleGetHighlightOptions(req);
                case  "getDegreeProgram" -> handleGetDegreeProgram(req);
                case "expandProgram" -> handleExpandProgram(req);
                case "collapseProgram" -> handleCollapseProgram(req);
                case "getProgramInputVariables" -> handleGetProgramInputVariables(req);
                case "runProgram" -> hanleRunProgram(req);
                case "getRunStatistic" -> handleGetRunStatistic(req);
                case "deductCredit" -> handleDeductCredits(req);
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

    private static BaseResponse handleRemoveUser(BaseRequest req) {
        String username = getString(req, "username");
        if (!validateParameter(username)) {
            return new BaseResponse(false, "Invalid username");
        }

        int result = EngineManager.getInstance().removeUser(username);
        return switch (result) {
            case ERROR_CODES.ERROR_USER_NOT_FOUND -> new BaseResponse(false, "User not logged in");
            case ERROR_CODES.ERROR_OK -> new BaseResponse(true, "Logged out successfully");
            default -> new BaseResponse(false, "Failed to logout");
        };
    }

    private static BaseResponse handleGetCredits(BaseRequest req) {
        String username = getString(req, "username");

        if (!validateParameter(username))
            return new BaseResponse(false, "Invalid username");

        int credits = EngineManager.getInstance().getCredit(username);
        if (credits == ERROR_CODES.ERROR_USER_EXISTS)
            return new BaseResponse(false, "User not logged in");
        if (credits < 0)
            return new BaseResponse(false, "Server error");

        return new BaseResponse(true, "Credits loaded").add("credits", credits);
    }

    private static BaseResponse handleGetDegreeProgram(BaseRequest req) {
        String username = getString(req, "username");

        if (!validateParameter(username))
            return new BaseResponse(false, "Invalid username");

        int degreeProgram = EngineManager.getInstance().getDegreeProgram(username);
        if (degreeProgram == ERROR_CODES.ERROR_USER_EXISTS)
            return new BaseResponse(false, "User not logged in");
        if (degreeProgram < 0)
            return new BaseResponse(false, "Server error");
        return new BaseResponse(true, "Degree program loaded").add("degree", degreeProgram);
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
        List<Map<String, Object>> statistics = new ArrayList<>();

        int result = EngineManager.getInstance().fetchUserStatistic(username, statistics);

        return switch (result) {
            case ERROR_CODES.ERROR_USER_NOT_FOUND -> new BaseResponse(false, "User not logged in");
            case ERROR_CODES.ERROR_OK -> new BaseResponse(true, "Statistics collected").add("execStatistics", statistics);
            default -> new BaseResponse(false, "Failed to fetch statistics");
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

    private static BaseResponse handleSendMessage(BaseRequest req) {
        String username = getString(req, "username");
        String message = getString(req, "message");

        if (!validateParameter(username)) {
            return new BaseResponse(false, "Invalid username");
        }

        if (!validateParameter(message)) {
            return new BaseResponse(false, "Invalid message");
        }

        int result = EngineManager.getInstance().sendMessage(username, message);
        return switch (result) {
            case ERROR_CODES.ERROR_USER_NOT_FOUND -> new BaseResponse(false, "User not logged in");
            case ERROR_CODES.ERROR_OK -> new BaseResponse(true, "Message sent successfully");
            default -> new BaseResponse(false, "Failed to send message");
        };
    }

    private static BaseResponse handleGendMessage(BaseRequest req) {
        List<Map<String, Object>> messagesList = new ArrayList<>();
        int result = EngineManager.getInstance().fetchMessages(messagesList);
        if (result != ERROR_CODES.ERROR_OK)
            return new BaseResponse(false, "Failed to fetch messages");

        return new BaseResponse(true, "Messages fetched successfully").add("messages", messagesList);
    }

        private static BaseResponse handleExecuteProgram(BaseRequest req) {
        String username = getString(req, "username");
        String programName = getString(req, "programName");
        Boolean isProgram = getBoolean(req, "isProgram");

        if (!validateParameter(username)) {
            return new BaseResponse(false, "Invalid username");
        }

        if (!validateParameter(programName)) {
            return new BaseResponse(false, "Invalid programName");
        }

        if (!validateParameter(isProgram)) {
            return new BaseResponse(false, "Invalid isProgram");
        }

        int result = EngineManager.getInstance().setProgramToUser(username, programName, isProgram);
        return switch (result) {
            case ERROR_CODES.ERROR_PROGRAM_NOT_FOUND -> new BaseResponse(false, "Program not found");
            case ERROR_CODES.ERROR_FUNCTION_NOT_FOUND -> new BaseResponse(false, "Function not found");
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

        if (!validateParameter(username))
            return new BaseResponse(false, "Invalid username");

        if (!validateParameter(instructionNumber))
            return new BaseResponse(false, "Invalid instructionNumber");

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

    private static BaseResponse handleGetProgramFunctions(BaseRequest req) {
        String programName = getString(req, "programName");
        String username = getString(req, "username");

        if (!validateParameter(username))
            return new BaseResponse(false, "Invalid username");

        if (!validateParameter(programName)) {
            return new BaseResponse(false, "Invalid programName");
        }

        List<String> functions = new ArrayList<>();
        functions.add(programName);
        int result = EngineManager.getInstance().getProgramFunctions(username, functions);
        return switch (result) {
            case ERROR_CODES.ERROR_PROGRAM_NOT_FOUND -> new BaseResponse(false, "Program not found");
            case ERROR_CODES.ERROR_OK -> new BaseResponse(true, "Functions fetched")
                    .add("functions", functions);
            default -> new BaseResponse(false, "Server error");
        };
    }

    private static BaseResponse handleSetWokFunctionUser(BaseRequest req) {
        String username = getString(req, "username");
        String funcName = getString(req, "funcName");

        if (!validateParameter(username))
            return new BaseResponse(false, "Invalid username");
        if (!validateParameter(funcName))
            return new BaseResponse(false, "Invalid funcName");

        int result = EngineManager.getInstance().setWokFunctionToUser(username, funcName);
        return switch (result) {
            case ERROR_CODES.ERROR_USER_NOT_FOUND -> new BaseResponse(false, "User not found");
            case ERROR_CODES.ERROR_FUNCTION_NOT_FOUND -> new BaseResponse(false, "Function not found");
            case ERROR_CODES.ERROR_OK -> new BaseResponse(true, "Function set successfully");
            default -> new BaseResponse(false, "Server error");
        };
    }

    private static BaseResponse handleGetHighlightOptions(BaseRequest req) {
        String username = getString(req, "username");
        if (!validateParameter(username))
            return new BaseResponse(false, "Invalid username");
        List <String> options = new ArrayList<>();
        options.add("none");
        int result = EngineManager.getInstance().getHighlightOptions(username, options);
        return switch (result) {
            case ERROR_CODES.ERROR_USER_NOT_FOUND -> new BaseResponse(false, "User not found");
            case ERROR_CODES.ERROR_OK -> new BaseResponse(true, "Options fetched")
                    .add("highlightOptions", options);
            default -> new BaseResponse(false, "Server error");
        };
    }

    private static BaseResponse handleExpandProgram(BaseRequest req) {
        String username = getString(req, "username");
        if (!validateParameter(username))
            return new BaseResponse(false, "Invalid username");
        Integer degree = getInt(req, "degree");
        if (!validateParameter(degree) || degree < 0)
            return new BaseResponse(false, "Invalid degree");
        int result = EngineManager.getInstance().expandProgram(username, degree);
        return switch (result) {
            case ERROR_CODES.ERROR_USER_NOT_FOUND -> new BaseResponse(false, "User not found");
            case ERROR_CODES.ERROR_PROGRAM_NOT_FOUND -> new BaseResponse(false, "No program set for user");
            case ERROR_CODES.ERROR_OK -> new BaseResponse(true, "Program expanded successfully");
            default -> new BaseResponse(false, "Server error");
        };
    }

    private static BaseResponse handleCollapseProgram(BaseRequest req) {
        String username = getString(req, "username");
        if (!validateParameter(username))
            return new BaseResponse(false, "Invalid username");
        int result = EngineManager.getInstance().collapseProgram(username);
        return switch (result) {
            case ERROR_CODES.ERROR_USER_NOT_FOUND -> new BaseResponse(false, "User not found");
            case ERROR_CODES.ERROR_PROGRAM_NOT_FOUND -> new BaseResponse(false, "No program set for user");
            case ERROR_CODES.ERROR_OK -> new BaseResponse(true, "Program collapse successfully");
            default -> new BaseResponse(false, "Server error");
        };
    }

    private static BaseResponse handleGetProgramInputVariables(BaseRequest req) {
        String username = getString(req, "username");
        if (!validateParameter(username))
            return new BaseResponse(false, "Invalid username");
        List <String> variables = new ArrayList<>();
        int result = EngineManager.getInstance().getProgramInputVariables(username, variables);
        return switch (result) {
            case ERROR_CODES.ERROR_USER_NOT_FOUND -> new BaseResponse(false, "User not found");
            case ERROR_CODES.ERROR_PROGRAM_NOT_FOUND -> new BaseResponse(false, "No program set for user");
            case ERROR_CODES.ERROR_OK -> new BaseResponse(true, "Input variables fetched successfully")
                    .add("inputVariables", variables);
            default -> new BaseResponse(false, "Server error");
        };
    }

    private static BaseResponse hanleRunProgram(BaseRequest req) {
        String username = getString(req, "username");
        Boolean isDebugMode = getBoolean(req, "isDebugMode");
        int degree = getInt(req, "degree");

        if (!validateParameter(username))
            return new BaseResponse(false, "Invalid username");
        if (!validateParameter(isDebugMode))
            return new BaseResponse(false, "Invalid isDebug parameter");
        if (!validateParameter(degree))
            return new BaseResponse(false, "Invalid degree parameter");


        List<Long> userVars  = new ArrayList<>();
        Object varsObj = req.data.get("inputVariables");
        if (varsObj instanceof List<?> varsList) {
            for (Object var : varsList) {
                if (var instanceof Number num) {
                    userVars.add(num.longValue());
                } else {
                    return new BaseResponse(false, "Invalid input variable type");
                }
            }
        }
        List<ExecutionStep> executionDetails = new ArrayList<>();
        int result = EngineManager.getInstance().runProgram(username, userVars, executionDetails, degree, isDebugMode);
        return switch (result) {
            case ERROR_CODES.ERROR_USER_NOT_FOUND -> new BaseResponse(false, "User not found");
            case ERROR_CODES.ERROR_PROGRAM_NOT_FOUND -> new BaseResponse(false, "No program set for user");
            case ERROR_CODES.ERROR_INVALID_INPUT_VARIABLES -> new BaseResponse(false, "Invalid input variables");
            case ERROR_CODES.ERROR_NOT_ENOUGH_CREDIT -> new BaseResponse(false, "Insufficient credits");
            case ERROR_CODES.ERROR_OK -> new BaseResponse(true, "Program executed successfully").add("runListMap", executionDetails);
            default -> new BaseResponse(false, "Server error");
        };
    }

    private static BaseResponse handleGetRunStatistic(BaseRequest req) {
        String username = getString(req, "username");
        if (!validateParameter(username))
            return new BaseResponse(false, "Invalid username");

        List<RunResultProperty> runStatistics = new ArrayList<>();
        int result = EngineManager.getInstance().getRunStatistics(username, runStatistics);
        return switch (result) {
            case ERROR_CODES.ERROR_USER_NOT_FOUND -> new BaseResponse(false, "User not found");
            case ERROR_CODES.ERROR_PROGRAM_NOT_FOUND -> new BaseResponse(false, "No program set for user");
            case ERROR_CODES.ERROR_OK -> new BaseResponse(true, "Run statistics fetched successfully")
                    .add("runStatistics", runStatistics);
            default -> new BaseResponse(false, "Server error");
        };
    }

    private static BaseResponse handleDeductCredits(BaseRequest req) {
        String username = getString(req, "username");
        Integer amount = getInt(req, "cost");
        if (!validateParameter(username)) {
            return new BaseResponse(false, "Invalid username");
        }
        if (!validateParameter(amount)) {
            return new BaseResponse(false, "Invalid credits amount");
        }

        int newBalance = EngineManager.getInstance().deductCredits(username, amount);

        if (newBalance == ERROR_CODES.ERROR_USER_NOT_FOUND)
            return new BaseResponse(false, "User not logged in");
        if (newBalance == ERROR_CODES.ERROR_INVALID_CREDENTIALS)
            return new BaseResponse(false, "Invalid amount");
        if (newBalance == ERROR_CODES.ERROR_NOT_ENOUGH_CREDIT)
            return new BaseResponse(false, "Not enough credits");
        if (newBalance < 0)
            return new BaseResponse(false, "Server error");

        return new BaseResponse(true, "Deducted successfully").add("newBalance", newBalance);
    }

    // ---------------------- UTILITIES ----------------------
    private static boolean validateParameter(String param) {
        return (param != null && !param.isBlank());
    }

    private static boolean validateParameter(Integer param) {
        return (param != null);
    }

    private static boolean validateParameter(Boolean param) {
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

    private static Boolean getBoolean(BaseRequest req, String key) {
        Object value = req.data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
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
