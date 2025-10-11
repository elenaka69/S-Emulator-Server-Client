package server.engine.impl.api.basic;


import server.engine.impl.api.skeleton.AbstractOpBasic;
import server.engine.impl.api.skeleton.OpData;
import server.engine.label.*;
import server.engine.program.FunctionExecutor;
import server.engine.variable.VariableImpl;

import java.util.List;


public class OpNeutral extends AbstractOpBasic
{
    public OpNeutral(VariableImpl variable) {
        this( variable, FixedLabel.EMPTY,null);
    }

    public OpNeutral(VariableImpl variable, AbstractOpBasic parent) {
        this( variable, FixedLabel.EXIT, parent );
    }

    public OpNeutral(VariableImpl variable, Label label) {
        this( variable, label,null);
    }

    public OpNeutral( VariableImpl variable, Label label, AbstractOpBasic parent) {
        super(OpData.NEUTRAL, variable, label, parent);
        generateUniqId();
    }

    public List<AbstractOpBasic> expand(int extensionLevel, FunctionExecutor program) {
        return List.of(this);
    }

    @Override
    public List<AbstractOpBasic> expand(int ignoredExtensionLevel, FunctionExecutor ignoredProgram, VariableImpl Papa) {
       return expand(ignoredExtensionLevel,ignoredProgram);
    }

    @Override
    public Label execute(FunctionExecutor program) {
        program.increaseCycleCounter(getCycles());
        return FixedLabel.EMPTY;
    }

    //implementation of deep clone
    @Override
    public AbstractOpBasic myClone() {
        return new OpNeutral(getVariable().myClone(), getLabel().myClone());
    }
    @Override
    public String getRepresentation()
    {
        return String.format("%s ← %s", getVariable().getRepresentation(), getVariable().getRepresentation());
    }
}

