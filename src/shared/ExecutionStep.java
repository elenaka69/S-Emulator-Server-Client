package shared;

import server.engine.variable.VariableImpl;

import java.util.Map;
import java.util.TreeMap;

public class ExecutionStep {
    private int step;
    private Map<String, Long> variables;

    // Default constructor is required for Jackson
    public ExecutionStep() {}

    public ExecutionStep(int step, Map<String, Long> variables) {
        this.step = step;
        this.variables = variables;
    }

    public int getStep() { return step; }
    public void setStep(int step) { this.step = step; }

    public Map<String, Long> getVariables() { return variables; }
    public void setVariables(Map<String, Long> variables) { this.variables = variables; }
}
