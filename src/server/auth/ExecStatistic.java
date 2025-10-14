package server.auth;

import java.util.concurrent.atomic.AtomicInteger;

public class ExecStatistic {

    private final String type;
    private final String name;
    private final String arch;
    private final AtomicInteger degree = new AtomicInteger(0);
    private final AtomicInteger result = new AtomicInteger(0);
    private final AtomicInteger cycles = new AtomicInteger(0);

    public ExecStatistic(String type, String name, String arch, int degree, int result, int cycles) {
        this.type = type;
        this.name = name;
        this.arch = arch;
        this.degree.set(degree);
        this.result.set(result);
        this.cycles.set(cycles);
    }

    public String getType() { return type; }
    public String getName() { return name; }
    public String getArch() {  return arch; }
    public AtomicInteger getDegree() {  return degree; }
    public AtomicInteger getResult() { return result; }
    public AtomicInteger getCycles() { return cycles; }
}
