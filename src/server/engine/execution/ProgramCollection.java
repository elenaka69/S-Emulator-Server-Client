package server.engine.execution;

import server.engine.program.FunctionExecutor;
import server.engine.program.FunctionExecutorImpl;
import server.engine.program.SprogramImpl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProgramCollection {

    private static final Map<String, ProgramProperty> programs = new ConcurrentHashMap<>();

    private static final Map<String, FunctionExecutorImpl> functions = new ConcurrentHashMap<>();
    private static final List<String> listPrograms = new CopyOnWriteArrayList<>();
    private static final List<String> listFunctions = new CopyOnWriteArrayList<>();

    public static void registerProgram(String userName, String fileName, FunctionExecutor program) {
        // putIfAbsent returns the previous value, null if none
        ProgramProperty prop = new ProgramProperty(program, fileName, userName);

        if (programs.putIfAbsent(fileName, prop) == null) {
            listPrograms.add(fileName); // only add if it was absent
        }
    }

    public static SprogramImpl getProgram(String fileName) {
        ProgramProperty prop = programs.get(fileName);
        if (prop != null)
            return (SprogramImpl)(prop.getExecutor());
        return null;
    }

    public static Map<String, ProgramProperty> getPrograms() {
        return programs;
    }

    public static boolean isProgramExists(String fileName) {
        return programs.containsKey(fileName);
    }

    public static List<String> getListPrograms() {
        return listPrograms;
    }




    public static void registerFunction(String functionName, FunctionExecutorImpl function) {
        if (functions.putIfAbsent(functionName, function) == null) {
            listFunctions.add(functionName);
        }
    }

    public static void removeFunction(String functionName) {
        if (functions.remove(functionName) != null) {
            listFunctions.remove(functionName);
        }
    }

    public static FunctionExecutorImpl getFunction(String functionName) {
        return functions.get(functionName);
    }

    public static boolean isFunctionExists(String functionName) {
        return functions.containsKey(functionName);
    }

    public static List<String> getListFunctions() {
        return listFunctions;
    }
}
