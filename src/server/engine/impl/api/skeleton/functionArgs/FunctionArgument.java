package server.engine.impl.api.skeleton.functionArgs;

import java.util.List;

public class FunctionArgument extends AbstractArgument {
    private final String functionName;
    private final String strArguments;
    private final List<AbstractArgument> functionArguments;

    public FunctionArgument(String argument) {
        super(ArgumentTypes.FUNCTION);

        int idx = argument.indexOf(',');
        String funcArgs;
        functionName = extractFuncName(argument, idx);
        funcArgs = extractFuncArgs(argument, idx);
        if (!funcArgs.isEmpty())
            functionArguments = AbstractArgument.parseFunctionArguments(funcArgs);
        else
            functionArguments = null;
        strArguments = funcArgs;
    }

    private String extractFuncName(String input, int idx)
    {
        if (idx == -1)
            return input.trim();
        else
            return input.substring(0, idx).trim();
    }
    private String extractFuncArgs(String input, int idx)
    {
        if (idx == -1)
            return "";
        else
            return input.substring(idx + 1).trim();
    }

    public static String removeBrackets(String s) {
        if (s != null && s.length() >= 2
                && s.startsWith("(") && s.endsWith(")")) {
            return s.substring(1, s.length() - 1);
        }
        return s; // return unchanged if it doesn't match
    }

    public List<AbstractArgument> getArgument() {
        return functionArguments;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getStrArguments() {
        return strArguments;
    }
}
