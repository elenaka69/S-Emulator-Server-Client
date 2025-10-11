package server.engine.impl.api.basic;

import server.engine.impl.api.skeleton.AbstractOpBasic;
import server.engine.impl.api.skeleton.OpData;
import server.engine.label.*;
import server.engine.program.FunctionExecutor;
import server.engine.variable.VariableImpl;

import java.util.ArrayList;
import java.util.List;

public class OpDecrease extends AbstractOpBasic {

    public Label execute(FunctionExecutor program) {
        Long variableValue = program.getVariableValue(getVariable());
        variableValue = Math.max(0, variableValue - 1);
        ArrayList<VariableImpl> vars = new ArrayList<>();
        ArrayList<Long> vals = new ArrayList<>();
        vars.add(getVariable());
        vals.add(variableValue);

        program.addSnap(vars,vals);
        program.increaseCycleCounter(getCycles());

        return FixedLabel.EMPTY;
    }

    public OpDecrease(VariableImpl variable) {
        this( variable, FixedLabel.EMPTY, null);
    }
    public OpDecrease(VariableImpl variable, AbstractOpBasic parent) {
        this( variable, FixedLabel.EMPTY,parent);
    }
    public OpDecrease(VariableImpl variable, Label label) {
        this( variable, label, null);
    }
    public OpDecrease(VariableImpl variable, Label label, AbstractOpBasic parent) {
        super(OpData.DECREASE,variable, label  ,parent);
        generateUniqId();
    }
    //implementation of deep clone
    @Override
    public AbstractOpBasic myClone() {
        return new OpDecrease(getVariable().myClone(),getLabel().myClone());
    }

    @Override
    public List<AbstractOpBasic> expand(int extensionLevel, FunctionExecutor program) {
        return List.of(this);
    }

    @Override
    public String getRepresentation() {
        return String.format("%s ← %s - 1", getVariable().getRepresentation(), getVariable().getRepresentation()) ;
    }

    @Override
    public List<AbstractOpBasic> expand(int ignoredExtensionLevel, FunctionExecutor ignoredProgram, VariableImpl ignoredPapa)
    {
        return List.of(this);
    }

}
