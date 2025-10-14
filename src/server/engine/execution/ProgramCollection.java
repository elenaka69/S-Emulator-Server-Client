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
    private static final Map<String, FunctionProperty> functions = new ConcurrentHashMap<>();


    public static void registerProgram(String userName, String fileName, FunctionExecutor program) {

        ProgramProperty prop = new ProgramProperty(program, fileName, userName);
        programs.putIfAbsent(fileName, prop);
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


    public static void registerFunction(String userName, String functionName, String programName, FunctionExecutor function) {
        FunctionProperty prop = new FunctionProperty(function, functionName, userName, programName);
        functions.putIfAbsent(functionName, prop);
    }

    public static void updateFunctionStatistics(String functionName) {
        FunctionProperty prop = functions.get(functionName);
        if (prop != null ) {
            prop.updateStatistics();
        }
    }
    public static boolean isToUpdateFunction(String functionName) {
        FunctionProperty prop = functions.get(functionName);
        return prop.getToUpdate();
    }

    public static void removeFunction(String functionName) {
        functions.remove(functionName);
    }

    public static FunctionExecutorImpl getFunction(String functionName) {
        FunctionProperty prop = functions.get(functionName);
        if (prop != null)
            return (FunctionExecutorImpl)(prop.getExecutor());
        return null;
    }

    public static boolean isFunctionExists(String functionName) {
        return functions.containsKey(functionName);
    }

    public static int getNumFunctions() {
        return functions.size();
    }
    public static Map<String, FunctionProperty> getFunctions() {
        return functions;
    }
}
