package server.engine.impl.api.skeleton.functionArgs;

import server.engine.variable.VariableImpl;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractArgument {
    private final ArgumentTypes type;

    AbstractArgument(ArgumentTypes type) {
        this.type = type;
    }

    public ArgumentTypes getType() {
        return type;
    }

    public enum ArgumentTypes {
        VARIABLE,
        FUNCTION;
    }

    public static List<AbstractArgument> parseFunctionArguments(String arguments) {
        boolean isFunc = false;

        if (arguments.isEmpty())
            return null;

        List<AbstractArgument> functionArgs = new ArrayList<>();;

        arguments = arguments.trim();

        int depth = 0;
        StringBuilder current = new StringBuilder();

        AbstractArgument arg;

        for (char c : arguments.toCharArray()) {
            if (c == ',' && depth == 0) {
                if (current.length() > 0) {
                    arg = processPart(current, isFunc);
                    functionArgs.add(arg);
                    isFunc = false;
                    // result.add();
                    current.setLength(0);
                }
            } else {
                if (c == '(') { depth++; isFunc = true; }
                if (c == ')') depth--;
                current.append(c);
            }
        }

        if (current.length() > 0) {
            arg = processPart(current, isFunc);
            functionArgs.add(arg);
        }

        return functionArgs;
    }
    private static AbstractArgument processPart(StringBuilder current, boolean isFunc)
    {
        AbstractArgument arg;

        String part = current.toString().trim();
        String cleanStr = cleanPart(part);
        if (isFunc)
            arg = new FunctionArgument(cleanStr);
        else
            arg = new VariableArgument(new VariableImpl(part));

        return arg;
    }

    private static String cleanPart(String part) {
        // Remove only outer parentheses if they wrap the whole part
        if (part.startsWith("(") && part.endsWith(")")) {
            // check if parentheses are balanced
            int depth = 0;
            boolean wraps = true;
            for (int i = 0; i < part.length(); i++) {
                char c = part.charAt(i);
                if (c == '(') depth++;
                if (c == ')') depth--;
                if (depth == 0 && i < part.length() - 1) {
                    wraps = false;
                    break;
                }
            }
            if (wraps) {
                return part.substring(1, part.length() - 1);
            }
        }
        return part;
    }

}
