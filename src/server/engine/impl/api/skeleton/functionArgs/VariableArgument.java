package server.engine.impl.api.skeleton.functionArgs;

import server.engine.variable.VariableImpl;

public class VariableArgument extends AbstractArgument {
    private final VariableImpl variable;
    public VariableArgument(VariableImpl variable) {
        super(ArgumentTypes.VARIABLE);
        this.variable = variable;
    }

    public VariableImpl getVariable() {
        return variable;
    }

      public VariableImpl getArgument() {
        return variable;
    }
}
