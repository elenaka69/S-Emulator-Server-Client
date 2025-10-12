package server.auth;

import server.engine.program.FunctionExecutor;
import server.engine.program.SprogramImpl;

import java.time.LocalDateTime;

public class UserProfile {
    private static final int CREDIT_START = 20000;

    private final String username;
    private int credit = CREDIT_START;
    private int spentCredit = 0;
    private final LocalDateTime loginTime;
    private boolean isActive;
    private long lastActive;
    private int numberPrograms = 0;
    private int numberFunctions = 0;
    private int numberExecutions = 0;
    private static SprogramImpl choosenMainProgram = null;
    private static FunctionExecutor workingFunction = null;
    private static String  mainProgramName = null;

    public UserProfile(String username) {
        this.username = username;
        this.loginTime = LocalDateTime.now();
        this.lastActive = System.currentTimeMillis();
        this.isActive = true;
    }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) {
        isActive = active;
        if (active)
            updateLastActive();
    }

    public int getCredit() { return credit; }
    public void setCredit(int credit) { this.credit = credit; }
    public int getTotalSpentCredits() { return spentCredit; }
    public void deductCredit(int amount) { this.credit -= amount; this.spentCredit += amount; }

    public String getUsername() { return username; }
    public LocalDateTime getLoginTime() { return loginTime; }

    public long getLastActive() { return lastActive; }
    public void updateLastActive() { this.lastActive = System.currentTimeMillis(); }

    public int getNumberPrograms() { return numberPrograms; }
    public int getNumberFunctions() {  return numberFunctions; }
    public void incrementPrograms() { numberPrograms++; }
    public void incrementFunctions(int nFunc) { numberFunctions += nFunc; }

    public int getNumberExecutions() { return numberExecutions; }

    public void setNumberExecutions(int numberExecutions) { this.numberExecutions = numberExecutions; }

    public void setMainProgram(SprogramImpl program, String programName) {
        choosenMainProgram = program;
        workingFunction = choosenMainProgram;
        mainProgramName = programName;
    }

    public FunctionExecutor getWorkProgram() {
        return workingFunction;
    }
}
