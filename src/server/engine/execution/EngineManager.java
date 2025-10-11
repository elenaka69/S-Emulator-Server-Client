package server.engine.execution;

import server.engine.program.FunctionExecutorImpl;
import server.engine.program.SprogramImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EngineManager {
    private static final Map<String, SprogramImpl> programs = new HashMap<>();
    private static final Map<String, FunctionExecutorImpl> functions = new HashMap<>();
    private static List<String> listPrograms = new ArrayList<>();
    private static List<String> listFunctions = new ArrayList<>();

    public static void registerProgram(String fileName, SprogramImpl program) {
        programs.put(fileName, program);
        listPrograms.add(fileName);
    }

    public static SprogramImpl getProgram(String fileName) {
        return programs.get(fileName);
    }

    public static void registerFunction(String functionName, FunctionExecutorImpl function) {
        functions.put(functionName, function);
        listFunctions.add(functionName);
    }

    public static void removeFunction(String functionName) {
        functions.remove(functionName);
        listFunctions.remove(functionName);
    }

    public static FunctionExecutorImpl getFunction(String functionName) {
        return functions.get(functionName);
    }

    public static List<String> getListPrograms() {
        return listPrograms;
    }

    public static List<String> getListFunctions() {
        return listFunctions;
    }

    public static boolean isProgramExists(String fileName) {
        return programs.containsKey(fileName);
    }

    public static boolean isFunctionExists(String functionName) {
        return functions.containsKey(functionName);
    }
}
