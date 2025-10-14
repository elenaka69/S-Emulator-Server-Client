package server.engine.execution;

import server.auth.ExecStatistic;
import server.auth.UserManager;
import server.auth.UserProfile;
import server.engine.impl.api.skeleton.AbstractOpBasic;
import server.engine.input.XmlTranslator.Factory;
import server.engine.program.FunctionExecutor;
import server.engine.program.FunctionExecutorImpl;
import server.engine.program.SprogramImpl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class EngineManager {

    private static final EngineManager INSTANCE = new EngineManager();
    private final Factory factory;
    private final ReentrantLock addProgramLock = new ReentrantLock();

    private EngineManager() {
        this.factory = new Factory(); // Each singleton has one Factory instance
    }

    public static EngineManager getInstance() {
        return INSTANCE;
    }

    /**
     * Adds a program for a user. Atomic and thread-safe.
     */
    public int addProgram(String username, String fileName, String base64Data) {
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null || !profile.isActive()) {
            return ERROR_CODES.ERROR_USER_NOT_FOUND;
        }

        addProgramLock.lock();
        try {
            if (ProgramCollection.isProgramExists(fileName)) {
                return ERROR_CODES.ERROR_PROGRAM_EXISTS;
            }

            // Decode Base64 → bytes → InputStream
            byte[] fileBytes = Base64.getDecoder().decode(base64Data);
            try (InputStream xmlStream = new ByteArrayInputStream(fileBytes)) {

                int nFunctionsBefore = ProgramCollection.getListFunctions().size();
                int res = factory.loadProgramFromXml(fileName, xmlStream);
                int nFunctionsAfter = ProgramCollection.getListFunctions().size();

                if (nFunctionsAfter > nFunctionsBefore) {
                    UserManager.incrementFunctions(username, nFunctionsAfter - nFunctionsBefore);
                }

                if (res == ERROR_CODES.ERROR_OK) {
                    UserManager.incrementPrograms(username);
                    return ERROR_CODES.ERROR_OK;
                } else if (res == ERROR_CODES.ERROR_FUNCTION_MISSING) {
                    return ERROR_CODES.ERROR_FUNCTION_MISSING;
                } else {
                    return ERROR_CODES.ERROR_INVALID_FILE;
                }

            } catch (Exception e) {
                e.printStackTrace();
                return ERROR_CODES.ERROR_INVALID_FILE;
            }

        } finally {
            addProgramLock.unlock();
        }
    }

    public int setProgramToUser(String username, String programName) {
        UserProfile profile = UserManager.getActiveUsers().get(username);
        SprogramImpl program = ProgramCollection.getProgram(programName);

        if (profile == null) return ERROR_CODES.ERROR_USER_NOT_FOUND;
        if (program == null) return ERROR_CODES.ERROR_PROGRAM_NOT_FOUND;

        // Clone program for user
        profile.setMainProgram((SprogramImpl) program.myClone(), programName);
        return ERROR_CODES.ERROR_OK;
    }

    public int getProgramInstructions(String username, List<Map<String, Object>> instructions) {
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null) return ERROR_CODES.ERROR_USER_NOT_FOUND;

        FunctionExecutor program = profile.getWorkProgram();
        if (program == null) return ERROR_CODES.ERROR_PROGRAM_NOT_FOUND;

        int idx = 1;
        program.opListIndexReset();
        AbstractOpBasic op;
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

    public int getInstructionHistory(String username, Integer instructionNumber, List<Map<String, Object>> instructions) {
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null) return ERROR_CODES.ERROR_USER_NOT_FOUND;

        FunctionExecutor program = profile.getWorkProgram();
        if (program == null) return ERROR_CODES.ERROR_PROGRAM_NOT_FOUND;

        if (instructionNumber < 1 || instructionNumber > program.getOps().size()) {
            return ERROR_CODES.ERROR_INVALID_INSTRUCTION_NUMBER;
        }

        program.opListIndexReset();
        AbstractOpBasic targetOp = null;
        int idx = 1;
        AbstractOpBasic op;
        while ((op = program.getNextOp()) != null) {
            if (idx == instructionNumber) {
                targetOp = op;
                break;
            }
            idx++;
        }

        idx = 1;
        while (targetOp != null) {
            Map<String, Object> row = new HashMap<>();
            row.put("number", idx++);
            row.put("type", targetOp.getType());
            row.put("label", targetOp.getLabel().getLabelRepresentation());
            row.put("instruction", targetOp.getRepresentation());
            row.put("cycle", targetOp.getCycles());
            instructions.add(row);
            targetOp = targetOp.getParent();
        }

        return ERROR_CODES.ERROR_OK;
    }

    public int addUser(String username) {
        if (UserManager.isUserActive(username)) return ERROR_CODES.ERROR_USER_EXISTS;

        UserManager.addUser(username);
        return ERROR_CODES.ERROR_OK;
    }

    public int getCredit(String username) {
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null || !profile.isActive()) return ERROR_CODES.ERROR_USER_NOT_FOUND;

        return profile.getCredit();
    }

    public int chargeCredits(String username, Integer amount) {
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null || !profile.isActive()) return ERROR_CODES.ERROR_USER_NOT_FOUND;

        if (amount == null || amount <= 0) return ERROR_CODES.ERROR_INVALID_CREDENTIALS;

        synchronized (profile) {
            int newBalance = profile.getCredit() + amount;
            profile.setCredit(newBalance);
            return newBalance;
        }
    }

    public int logout(String username) {
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null || !profile.isActive()) return ERROR_CODES.ERROR_USER_NOT_FOUND;

        UserManager.logoutUser(username);
        return ERROR_CODES.ERROR_OK;
    }

    public int fetchUsers(List<Map<String, Object>> usersList) {

        int num = 1;
        for (Map.Entry<String, UserProfile> entry : UserManager.getActiveUsers().entrySet()) {
            UserProfile profile = entry.getValue();
            if (!profile.isActive()) continue;
            Map<String, Object> row = new HashMap<>();
            row.put("number", num++);
            row.put("userName", entry.getKey());
            row.put("uploadedPrograms", profile.getNumberPrograms());
            row.put("uploadedFunctions", profile.getNumberFunctions());
            row.put("creditBalance", profile.getCredit());
            row.put("spentCredits", profile.getTotalSpentCredits());
            row.put("executions", profile.getNumberExecutions());
            usersList.add(row);
        }

        return ERROR_CODES.ERROR_OK;
    }

    public int fetchUserStatistic(String username, List<Map<String, Object>> statistics) {
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null || !profile.isActive()) return ERROR_CODES.ERROR_USER_NOT_FOUND;

        synchronized (profile) {
            int num = 1;
            List<ExecStatistic> stat = profile.getExecutionStats();
            for (ExecStatistic s : stat) {
                Map<String, Object> row = new HashMap<>();
                row.put("number", num++);
                row.put("type", s.getType());
                row.put("name", s.getName());
                row.put("arch", s.getArch());
                row.put("degree", s.getDegree());
                row.put("result", s.getResult());
                row.put("cycles", s.getCycles());
                statistics.add(row);
            }
        }
        return ERROR_CODES.ERROR_OK;
    }

    public int fetchPrograms(List<Map<String, Object>> programList) {
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

    public int fetchFunctions(List<Map<String, Object>> functionsList) {
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

    public int getProgramFunctions(String programName, List<String> functionNames) {
        SprogramImpl program = ProgramCollection.getProgram(programName);
        if (program == null) return ERROR_CODES.ERROR_PROGRAM_NOT_FOUND;
        functionNames.addAll(program.getFuncNameList());;
        return ERROR_CODES.ERROR_OK;
    }

    public int setWokFunctionToUser(String username, String funcName) {
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null || !profile.isActive()) return ERROR_CODES.ERROR_USER_NOT_FOUND;

        return profile.setWorkFunction(funcName);
    }

    public int getHighlightOptions(String username, List<String> options) {
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null || !profile.isActive()) return ERROR_CODES.ERROR_USER_NOT_FOUND;

        return profile.fillHighlightOptions(options);
    }

    public int getDegreeProgram(String username) {
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null || !profile.isActive()) return ERROR_CODES.ERROR_USER_NOT_FOUND;

        return profile.getDgreeProgram();
    }

    public int expandProgram(String username, Integer degree) {
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null || !profile.isActive()) return ERROR_CODES.ERROR_USER_NOT_FOUND;
        return profile.expandProgram(degree);
    }

    public int collapseProgram(String username) {
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null || !profile.isActive()) return ERROR_CODES.ERROR_USER_NOT_FOUND;
        return profile.collapseProgram();
    }

    public int removeUser(String username) {
        UserProfile profile = UserManager.getActiveUsers().get(username);
        if (profile == null || !profile.isActive()) return ERROR_CODES.ERROR_USER_NOT_FOUND;

        UserManager.removeUser(username);
        return ERROR_CODES.ERROR_OK;
    }
}
