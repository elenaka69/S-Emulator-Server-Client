package server.engine.impl.api.skeleton;

import server.engine.variable.VariableImpl;

public interface VariableUser {
    public void setSecondaryVariable(VariableImpl variable);
    public VariableImpl getSecondaryVariable();
}
