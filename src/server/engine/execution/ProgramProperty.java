package server.engine.execution;

import server.engine.program.FunctionExecutor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ProgramProperty {
    private final String name;
    private final String username;
    private final AtomicInteger numInstructions = new AtomicInteger(0);
    private final AtomicInteger maxCost = new AtomicInteger(0);
    private final AtomicInteger numExecs = new AtomicInteger(0);
    private final AtomicInteger averageCost = new AtomicInteger(0);
    private final FunctionExecutor executor;
    private final AtomicBoolean toUpdate = new AtomicBoolean(false);


    public ProgramProperty(FunctionExecutor executor, String name, String username, boolean toUpdate) {
        this.executor = executor;
        this.name = name;
        this.username = username;
        this.toUpdate.set(toUpdate);
        if (!toUpdate) {
            this.numInstructions.set(executor.getNumInstuctions());
            this.maxCost.set(executor.getCost());
            this.averageCost.set(executor.getAverageCost());
        }
    }
    public void updateStatistics() {
        this.numInstructions.set(executor.getNumInstuctions());
        this.maxCost.set(executor.getCost());
        this.averageCost.set(executor.getAverageCost());
        this.toUpdate.set(false);
    }

    public String getName() { return name; }
    public String getUsername() { return username; }
    public int getNumInstructions() { return numInstructions.get(); }
    public int getMaxCost() { return maxCost.get(); }
    public int getNumExecs() { return numExecs.get(); }
    public int getAverageCost() { return averageCost.get(); }
    public FunctionExecutor getExecutor() { return executor; }
    public void incrementNumExecs() { numExecs.incrementAndGet();}
    public boolean isToUpdate() { return toUpdate.get(); }
}
