package server.auth;

import javafx.util.Pair;
import server.engine.execution.ERROR_CODES;
import server.engine.execution.ProgramExecutorImpl;
import server.engine.label.Label;
import server.engine.program.FunctionExecutor;
import server.engine.program.SprogramImpl;
import server.engine.variable.VariableImpl;
import shared.ExecutionStep;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class UserProfile {
    private static final int CREDIT_START = 20000;

    private final String username;
    private final LocalDateTime loginTime;

    private final AtomicInteger credit = new AtomicInteger(CREDIT_START);
    private final AtomicInteger spentCredit = new AtomicInteger(0);
    private final AtomicInteger numberPrograms = new AtomicInteger(0);
    private final AtomicInteger numberFunctions = new AtomicInteger(0);
     private final AtomicLong lastActive = new AtomicLong(System.currentTimeMillis());
    private final AtomicBoolean isActive = new AtomicBoolean(true);

    private final List<ExecStatistic> executions = new CopyOnWriteArrayList<>();

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

    public void deductCredit(int amount) {
        if (amount > 0) {
            credit.addAndGet(-amount);
            spentCredit.addAndGet(amount);
        }
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

    public int executeProgram(List<Long> userVars, List<ExecutionStep> executionDetails) {

        List<Pair<Integer, TreeMap<VariableImpl, Long>>> execResult =
                ProgramExecutorImpl.run(workingFunction, userVars, chosenMainProgram.getFunctions());

        for (Pair<Integer, TreeMap<VariableImpl, Long>> pair : execResult) {
            Map<String, Long> varMap = new LinkedHashMap<>();
            for (Map.Entry<VariableImpl, Long> entry : pair.getValue().entrySet()) {
                varMap.put(entry.getKey().getRepresentation(), entry.getValue());  // assuming VariableImpl has getName()
            }
            executionDetails.add(new ExecutionStep(pair.getKey(), varMap));
        }
        return ERROR_CODES.ERROR_OK;
    }

    public int getRunStatistics(List<Long> runStatistics) {
        if (workingFunction != null) {
            runStatistics.add(workingFunction.getVariableValue(VariableImpl.RESULT));
            runStatistics.add((long) workingFunction.getCycles());
            return ERROR_CODES.ERROR_OK;
        }
        return ERROR_CODES.ERROR_FUNCTION_NOT_FOUND;
    }
}

