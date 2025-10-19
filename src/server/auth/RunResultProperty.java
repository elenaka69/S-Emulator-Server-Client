package server.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RunResultProperty {
    private final AtomicInteger degree;
    private final String inputVars;
    private final AtomicLong result;
    private final AtomicInteger cycles;

    @JsonCreator
    public RunResultProperty(
            @JsonProperty("degree") int degree,
            @JsonProperty("inputVars") String inputVars,
            @JsonProperty("result") long result,
            @JsonProperty("cycles") int cycles
    ) {
        this.degree = new AtomicInteger(degree);
        this.inputVars = inputVars;
        this.result = new AtomicLong(result);
        this.cycles = new AtomicInteger(cycles);
    }

    public int getDegree() { return degree.get(); }
    public String getInputVars() { return inputVars; }
    public long getResult() { return result.get(); }
    public int getCycles() { return cycles.get(); }
}

