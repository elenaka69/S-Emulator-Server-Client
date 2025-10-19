package server.auth;

import javafx.scene.control.TextField;
import server.engine.execution.ERROR_CODES;
import server.engine.execution.EngineManager;
import server.engine.execution.ProgramCollection;
import server.engine.impl.api.skeleton.AbstractOpBasic;
import server.engine.impl.api.synthetic.OpFunctionBase;
import server.engine.label.Label;
import server.engine.program.FunctionExecutor;
import server.engine.program.FunctionExecutorImpl;
import server.engine.program.SprogramImpl;
import server.engine.variable.VariableImpl;
import shared.ExecutionStep;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class UserProfile {
    private static final int CREDIT_START = 0;

    private final String username;
    private final LocalDateTime loginTime;

    private final AtomicInteger credit = new AtomicInteger(CREDIT_START);
    private final AtomicInteger spentCredit = new AtomicInteger(0);
    private final AtomicInteger numberPrograms = new AtomicInteger(0);
    private final AtomicInteger numberFunctions = new AtomicInteger(0);
     private final AtomicLong lastActive = new AtomicLong(System.currentTimeMillis());
    private final AtomicBoolean isActive = new AtomicBoolean(true);

    private final List<ExecStatistic> executions = new CopyOnWriteArrayList<>();
    private static final Map<String, List<RunResultProperty>> runResults = new ConcurrentHashMap<>();

    private volatile SprogramImpl chosenMainProgram = null;
    private volatile FunctionExecutor workingFunction = null;
    private volatile String mainProgramName = null;

    public UserProfile(String username) {
        this.username = username;
        this.loginTime = LocalDateTime.now();
    }
    public boolean isActive() { return isActive.get(); }
    public void setActive(boolean active) {
        isActive.set(active);
        if (active)
            updateLastActive();
    }

    public long getLastActive() { return lastActive.get(); }
    public void updateLastActive() { this.lastActive.set(System.currentTimeMillis()); }

    public int getCredit() { return credit.get(); }

    public void setCredit(int amount) { credit.set(amount); }

    public int getTotalSpentCredits() { return spentCredit.get(); }

    public int deductCredit(int amount) {
        if (amount > 0) {
            if (amount > credit.get())
                return ERROR_CODES.ERROR_NOT_ENOUGH_CREDIT;
            credit.addAndGet(-amount);
            spentCredit.addAndGet(amount);
        }
        return ERROR_CODES.ERROR_OK;
    }

    public String getUsername() { return username; }
    public LocalDateTime getLoginTime() { return loginTime; }

    public int getNumberPrograms() { return numberPrograms.get(); }
    public int getNumberFunctions() { return numberFunctions.get(); }
    public int getNumberExecutions() { return executions.size(); }
    public void incrementPrograms() { numberPrograms.incrementAndGet(); }
    public void incrementFunctions(int nFunc) {
        if (nFunc > 0) numberFunctions.addAndGet(nFunc);
    }

    public void addExecutionStat(String type, String name, String arch, int degree, int result, int cycles) {
        executions.add(new ExecStatistic(type, name, arch, degree, result, cycles));
    }
    public List<ExecStatistic> getExecutionStats() { return executions; }

    public synchronized void setMainProgram(SprogramImpl program, String programName) {
        this.chosenMainProgram = program;
        this.workingFunction = program;
        this.mainProgramName = programName;
        chosenMainProgram.updateFunctions();
    }

    public SprogramImpl getMainProgram() {
        return chosenMainProgram;
    }

    public FunctionExecutor getWorkProgram() {
        return workingFunction;
    }

    public String getMainProgramName() {
        return mainProgramName;
    }

    public int setWorkFunction(String funcName) {
        if (funcName.equals(mainProgramName)) {
            workingFunction = chosenMainProgram;
            return ERROR_CODES.ERROR_OK;
        }
        Set<String> funcNameList = chosenMainProgram.getFuncNameList();

        for (String func : funcNameList) {
            if ( func.equals(funcName) ) {
                workingFunction = chosenMainProgram.getFunction(funcName);
                 return ERROR_CODES.ERROR_OK;
            }
        }
        return ERROR_CODES.ERROR_FUNCTION_NOT_FOUND;
    }

    public int fillHighlightOptions(List<String> options) {
        for (VariableImpl v : workingFunction.getAllVars()) {
            options.add(v.getRepresentation());
        }
        for (Label label : workingFunction.getLabelSet()) {
            options.add(label.getLabelRepresentation());
        }
        return ERROR_CODES.ERROR_OK;
    }

    public int getInputVariables(List<String> variables) {
        if (workingFunction != null) {
            for (int i = 0; i < workingFunction.getInputVarSize(); i++) {
                String varName = workingFunction.getNextVar(i).getRepresentation();
                variables.add(varName);
            }
            return ERROR_CODES.ERROR_OK;
        }
        return ERROR_CODES.ERROR_FUNCTION_NOT_FOUND;
    }

    public int getDgreeProgram() {
        if (workingFunction != null)
            return workingFunction.getProgramDegree();
        return 0;
    }

    public int expandProgram(Integer degree) {
        if (workingFunction != null) {
            workingFunction.expandProgram(degree);
            return ERROR_CODES.ERROR_OK;
        }
        return ERROR_CODES.ERROR_FUNCTION_NOT_FOUND;
    }

    public int collapseProgram() {
        if (workingFunction != null) {
            workingFunction.collapse();
            return ERROR_CODES.ERROR_OK;
        }
        return ERROR_CODES.ERROR_FUNCTION_NOT_FOUND;
    }

    private void addExecStatistic(List<ExecutionStep> executionDetails, int degree) {

        String type = workingFunction.getUserString() == null ? "program" : "function";
        String name = workingFunction.getUserString() == null ? workingFunction.getName() : workingFunction.getUserString();
        String arch;
        switch (workingFunction.getCost()) {
            case 5: arch = "I"; break;
            case 100: arch = "II"; break;
            case 500: arch = "III"; break;
            case 1000: arch = "IV"; break;
            default: arch = "Unknown"; break;
        }

        long result = workingFunction.getVariableValue(VariableImpl.RESULT);

        executions.add(new ExecStatistic(type, name, arch, degree, (int) result, workingFunction.getCycles()));
    }

    private void addExecResults(List<Long> userVars, int degree) {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < workingFunction.getInputVarSize(); i++) {
            String varName = workingFunction.getNextVar(i).getRepresentation();
            sb.append(varName).append(" = ").append(userVars.get(i)).append("   ");
        }
        RunResultProperty result = new RunResultProperty(degree, sb.toString(),
                workingFunction.getVariableValue(VariableImpl.RESULT),
                workingFunction.getCycles());

        runResults.computeIfAbsent(workingFunction.getName(), k -> Collections.synchronizedList(new ArrayList<>()))
                .add(result);
    }

    public int executeProgram(List<Long> userVars, List<ExecutionStep> executionDetails, int degree, Boolean isDebugMode) {

        if (workingFunction == null)
            return ERROR_CODES.ERROR_FUNCTION_NOT_FOUND;

        if (deductCredit(workingFunction.getCost()) != ERROR_CODES.ERROR_OK)
            return ERROR_CODES.ERROR_NOT_ENOUGH_CREDIT;

        int result =  workingFunction.run(userVars, chosenMainProgram.getFunctions(), executionDetails, this,  isDebugMode);

        addExecStatistic(executionDetails, degree);
        addExecResults(userVars, degree);
        EngineManager.getInstance().incrementProgramExecs(mainProgramName, workingFunction.getCycles());

        return result;
    }

    public int getRunStatistics(List<RunResultProperty> runStatistics) {
        if (workingFunction != null) {
            runStatistics.addAll(runResults.getOrDefault(workingFunction.getName(), Collections.emptyList()));
            return ERROR_CODES.ERROR_OK;
        }

        return ERROR_CODES.ERROR_FUNCTION_NOT_FOUND;
    }

    public int setFunctionAsMainProgram(FunctionExecutor function, String programName) {
        Set<String> sunFunctions = new HashSet<>();

        for (AbstractOpBasic op : function.getOps()) {
            if (op instanceof OpFunctionBase) {
                String funName = ((OpFunctionBase) op).getFunctionName();
                String functionArgs = ((OpFunctionBase) op).getStrFunctionArguments();
                extractFunctions(funName, functionArgs, sunFunctions);
            }
        }

        chosenMainProgram = new SprogramImpl((FunctionExecutorImpl) function);

        chosenMainProgram.setFunctions(sunFunctions);

        chosenMainProgram.updateFunctions();

        this.workingFunction = chosenMainProgram;
        this.mainProgramName = programName;

        return ERROR_CODES.ERROR_OK;
    }

    private int extractFunctions(String funcName, String funcArgs, Set<String> functions) {

        functions.add(funcName);

        int i = 0;
        int len = funcArgs.length();
        while (true) {
            String word = "";
            // Find next '('
            int start = funcArgs.indexOf('(', i);
            if (start == -1) break;

            // Move to first character after '('
            int pos = start + 1;

            while (pos < len && Character.isWhitespace(funcArgs.charAt(pos))) {
                pos++;
            }

            // if next char is '(' then there's no word right after this '('
            if (pos < len && funcArgs.charAt(pos) == '(') {
                i = start + 1; // continue search after this '('
                continue;
            }

            // Extract a word until ',' or ')' or whitespace
            int wordStart = pos;
            while (pos < len &&
                    funcArgs.charAt(pos) != ',' &&
                    funcArgs.charAt(pos) != ')' &&
                    !Character.isWhitespace(funcArgs.charAt(pos))) {
                pos++;
            }

            if (wordStart < pos) {
                word = funcArgs.substring(wordStart, pos);
                if (!ProgramCollection.isFunctionExists(word))
                    return ERROR_CODES.ERROR_FUNCTION_MISSING;
                functions.add(word);
            }

            // continue searching for the next '(' after current '('
            i = start + 1;
        }

        return ERROR_CODES.ERROR_OK;
    }

    public int getProgramFunctions(List<String> functionNames) {
        if (chosenMainProgram != null) {
            Set<String> funcNameList = chosenMainProgram.getFuncNameList();
            functionNames.addAll(funcNameList);
            return ERROR_CODES.ERROR_OK;
        }
        return ERROR_CODES.ERROR_PROGRAM_NOT_FOUND;

    }
}

