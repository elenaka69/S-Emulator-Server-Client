package server.engine.execution;

import javafx.util.Pair;
import server.engine.program.FunctionExecutor;
import server.engine.variable.VariableImpl;

import java.util.*;

public class ProgramExecutorImpl {

    public ProgramExecutorImpl() { }

    public static List<Pair<Integer, TreeMap<VariableImpl, Long>>> run(FunctionExecutor program, List<Long> inputs, List <FunctionExecutor> functions) {
        return program.run(inputs, functions);
    }
}
