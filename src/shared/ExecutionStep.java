package shared;

import java.util.Map;


public class ExecutionStep {
    private int step;
    private Map<String, Long> variables;
    int stepCost;

    // Default constructor is required for Jackson
    public ExecutionStep() {}

    public ExecutionStep(int step, Map<String, Long> variables, int stepCost) {
        this.step = step;
        this.variables = variables;
        this.stepCost = stepCost;
    }


    public int getStep() { return step; }
    public void setStep(int step) { this.step = step; }

    public int getStepCost() { return stepCost; }
    public void setStepCost(int stepCost) { this.stepCost = stepCost; }

    public Map<String, Long> getVariables() { return variables; }
    public void setVariables(Map<String, Long> variables) { this.variables = variables; }
}
