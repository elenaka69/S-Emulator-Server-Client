package server.engine.execution;

import server.engine.program.FunctionExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ProgramProperty {
    private final String name;
    private final String username;
    private final AtomicInteger numInstructions = new AtomicInteger(0);
    private final AtomicInteger maxCost = new AtomicInteger(0);
    private final AtomicInteger averageCost = new AtomicInteger(0);
    private final List<Integer> execCosts;
    private final FunctionExecutor executor;

    public ProgramProperty(FunctionExecutor executor, String name, String username) {
        this.executor = executor;
        this.name = name;
        this.username = username;
        this.execCosts = new ArrayList<>();

        this.numInstructions.set(executor.getNumInstuctions());
        this.maxCost.set(executor.getCost());
    }

    public String getName() { return name; }
    public String getUsername() { return username; }
    public int getNumInstructions() { return numInstructions.get(); }
    public int getMaxCost() { return maxCost.get(); }
    public int getNumExecs() { return execCosts.size(); }
    public int getAverageCost() { return averageCost.get(); }
    public FunctionExecutor getExecutor() { return executor; }
    public void incrementExecs(int cycles) {
        execCosts.add(cycles);
        int total = execCosts.stream().mapToInt(Integer::intValue).sum();
        averageCost.set(total / execCosts.size());
    }
}
