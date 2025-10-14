package server.engine.execution;

import server.engine.program.FunctionExecutor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FunctionProperty {
    private final String name;
    private final String username;
    private final String programName;
    private final AtomicInteger numInstructions = new AtomicInteger(0);
    private final AtomicInteger maxCost = new AtomicInteger(0);
    private final FunctionExecutor executor;
    private final AtomicBoolean toUpdate = new AtomicBoolean(true);

    public FunctionProperty(FunctionExecutor executor, String name, String username, String programName) {
        this.executor = executor;
        this.name = name;
        this.username = username;
        this.programName = programName;
    }
    public void updateStatistics() {
        this.numInstructions.set(executor.getNumInstuctions());
        this.maxCost.set(executor.getCost());
        this.toUpdate.set(false);
    }
    public String getName() { return name; }
    public String getUsername() { return username; }

    public String getProgramName() { return programName; }

    public int getNumInstructions() { return numInstructions.get(); }
    public int getMaxCost() { return maxCost.get(); }
    public FunctionExecutor getExecutor() { return executor; }
    public boolean getToUpdate() { return toUpdate.get(); }
}
