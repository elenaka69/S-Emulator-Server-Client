package server.auth;

import server.engine.program.FunctionExecutor;
import server.engine.program.SprogramImpl;

import java.time.LocalDateTime;
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
    private final AtomicInteger numberExecutions = new AtomicInteger(0);
    private final AtomicLong lastActive = new AtomicLong(System.currentTimeMillis());
    private final AtomicBoolean isActive = new AtomicBoolean(true);

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
    public int getNumberExecutions() { return numberExecutions.get(); }
    public void incrementPrograms() { numberPrograms.incrementAndGet(); }
    public void incrementFunctions(int nFunc) {
        if (nFunc > 0) numberFunctions.addAndGet(nFunc);
    }
    public void incrementExecutions() { numberExecutions.incrementAndGet(); }

    public synchronized void setMainProgram(SprogramImpl program, String programName) {
        this.chosenMainProgram = program;
        this.workingFunction = program;
        this.mainProgramName = programName;
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
}
