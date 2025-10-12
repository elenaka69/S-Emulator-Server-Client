package server.engine.execution;

import server.auth.UserManager;
import server.auth.UserProfile;
import server.engine.impl.api.skeleton.AbstractOpBasic;
import server.engine.input.XmlTranslator.Factory;
import server.engine.program.FunctionExecutor;
import server.engine.program.FunctionExecutorImpl;
import server.engine.program.SprogramImpl;
import shared.BaseResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EngineManager {
    private static Factory factory = new Factory();

    public static int addProgram(String username, String fileName, String base64Data) {

        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null || !profile.isActive()) {
            return ERROR_CODES.ERROR_USER_NOT_FOUND;
        }

        // Decode Base64 → bytes → InputStream
        byte[] fileBytes = Base64.getDecoder().decode(base64Data);
        try (InputStream xmlStream = new ByteArrayInputStream(fileBytes)) {
            if (ProgramCollection.isProgramExists(fileName)) {
                return ERROR_CODES.ERROR_PROGRAM_EXISTS;
            }

            int res;
            int nFunctionsBefore = ProgramCollection.getListFunctions().size();

            res = factory.loadProgramFromXml(fileName, xmlStream);

            int nFunctionsAfter = ProgramCollection.getListFunctions().size();
            if (nFunctionsAfter > nFunctionsBefore)
                UserManager.incrementFunctions(username, nFunctionsAfter - nFunctionsBefore);

            if (res == ERROR_CODES.ERROR_OK) {
                UserManager.incrementPrograms(username);
            } else if (res == ERROR_CODES.ERROR_FUNCTION_MISSING) {
                return ERROR_CODES.ERROR_FUNCTION_MISSING;
            }
            return ERROR_CODES.ERROR_OK;
        } catch (Exception e) {
            e.printStackTrace();
            return ERROR_CODES.ERROR_INVALID_FILE;
        }
    }

    public static int setProgramToUser(String username, String programName) {
        SprogramImpl program = ProgramCollection.getProgram(programName);
        if (program == null) {
            return ERROR_CODES.ERROR_PROGRAM_NOT_FOUND;
        }
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null) {
            return ERROR_CODES.ERROR_USER_NOT_FOUND;
        }
        profile.setMainProgram((SprogramImpl)program.myClone(), programName);
        return ERROR_CODES.ERROR_OK;
    }

    public static int getProgramInstructions(String username, List<Map<String, Object>> instructions) {
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null) {
            return ERROR_CODES.ERROR_USER_NOT_FOUND;
        }
        FunctionExecutor program = profile.getWorkProgram();
        if (program == null) {
            return ERROR_CODES.ERROR_PROGRAM_NOT_FOUND;
        }
        int idx = 1;
        AbstractOpBasic op;
        program.opListIndexReset();
        while ((op = program.getNextOp()) != null) {
            Map<String, Object> row = new HashMap<>();
            row.put("number", idx++);
            row.put("type", op.getType());
            row.put("label", op.getLabel().getLabelRepresentation());
            row.put("instruction", op.getRepresentation());
            row.put("cycle", op.getCycles());
            instructions.add(row);
        }

        return ERROR_CODES.ERROR_OK;
    }

    public static int getInstructionHistory(String username, Integer instructionNumber, List<Map<String, Object>> instructions) {
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null) {
            return ERROR_CODES.ERROR_USER_NOT_FOUND;
        }
        FunctionExecutor program = profile.getWorkProgram();
        if (program == null) {
            return ERROR_CODES.ERROR_PROGRAM_NOT_FOUND;
        }
        if (instructionNumber < 1 || instructionNumber > program.getOps().size()) {
            return ERROR_CODES.ERROR_INVALID_INSTRUCTION_NUMBER;
        }

        int idx = 1;
        AbstractOpBasic op;
        program.opListIndexReset();
        while ((op = program.getNextOp()) != null) {
            if (idx == instructionNumber) {
                break;
            }
            idx++;
        }

        idx = 1;
        AbstractOpBasic currOp = op;
        while(currOp != null) {
            Map<String, Object> row = new HashMap<>();
            row.put("number", idx++);
            row.put("type", currOp.getType());
            row.put("label", currOp.getLabel().getLabelRepresentation());
            row.put("instruction", currOp.getRepresentation());
            row.put("cycle", currOp.getCycles());
            currOp = currOp.getParent();
            instructions.add(row);
        }
        return ERROR_CODES.ERROR_OK;
    }

    public static int addUser(String username) {
        if (UserManager.isUserActive(username)) {
           return ERROR_CODES.ERROR_USER_EXISTS;
        }

        UserManager.addUser(username);
        return ERROR_CODES.ERROR_OK;
    }

    public static int getCreadit(String username) {
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null || !profile.isActive()) {
            return ERROR_CODES.ERROR_USER_NOT_FOUND;
        }
        int credits = profile.getCredit();
        return credits;
    }

    public static int ChargeCredits(String username, Integer amount) {
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null || !profile.isActive()) {
            return ERROR_CODES.ERROR_USER_NOT_FOUND;
        }

        if (amount == null || amount <= 0) {
            return ERROR_CODES.ERROR_INVALID_CREDENTIALS;
        }

        int newBalance = profile.getCredit() + amount;
        profile.setCredit(newBalance);

        return newBalance;
    }

    public static int fetchUsers(List<Map<String, Object>> usersList) {
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
        return ERROR_CODES.ERROR_OK;
    }

    public static int fetchUserStatistic(String username, Map<String, Object> statistics) {
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null || !profile.isActive()) {
            return ERROR_CODES.ERROR_USER_NOT_FOUND;
        }
        statistics .put("UserName", username);
        statistics .put("Number of Uploaded Programs", String.valueOf(profile.getNumberPrograms()));
        statistics .put("Number of Uploaded Functions", String.valueOf(profile.getNumberFunctions()));
        statistics .put("Current Credit Balance", String.valueOf(profile.getCredit()));
        statistics .put("Total Spent Credits", String.valueOf(profile.getTotalSpentCredits()));
        statistics .put("Total number of Executions", String.valueOf(profile.getNumberExecutions()));
        return ERROR_CODES.ERROR_OK;
    }

    public static int logout(String username) {
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null || !profile.isActive()) {
            return ERROR_CODES.ERROR_USER_NOT_FOUND;
        }
        UserManager.logoutUser(username);
        return ERROR_CODES.ERROR_OK;
    }

    public static int fetchPrograms(List<Map<String, Object>> programList) {
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
        return ERROR_CODES.ERROR_OK;
    }

    public static int fetchFunctions(List<Map<String, Object>> functionsList) {
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
        return ERROR_CODES.ERROR_OK;
    }
}
