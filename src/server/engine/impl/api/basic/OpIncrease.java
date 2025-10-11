package server.engine.impl.api.basic;

import server.engine.impl.api.skeleton.AbstractOpBasic;
import server.engine.impl.api.skeleton.OpData;
import server.engine.label.*;
import server.engine.program.FunctionExecutor;
import server.engine.variable.VariableImpl;

import java.util.ArrayList;
import java.util.List;

public class OpIncrease extends AbstractOpBasic {

    public OpIncrease(VariableImpl variable) {
        this( variable, FixedLabel.EMPTY, null);
    }
    public OpIncrease(VariableImpl variable, AbstractOpBasic parent) {
        this( variable,FixedLabel.EMPTY,parent);
    }
    public OpIncrease(VariableImpl variable, Label label) {
        this( variable, label,null);
    }
    public OpIncrease(VariableImpl variable, Label label, AbstractOpBasic parent) {
        super(OpData.INCREASE,variable,label  ,parent);
        generateUniqId();
    }

    @Override
    public Label execute(FunctionExecutor program) {

        Long variableValue = program.getVariableValue(getVariable());
        variableValue++;
        ArrayList<VariableImpl> vars = new ArrayList<>();
        ArrayList<Long> vals = new ArrayList<>();
        vars.add(getVariable());
        vals.add(variableValue);

        program.addSnap(vars,vals);
        program.increaseCycleCounter(getCycles());

        return FixedLabel.EMPTY;
    }
    public List<AbstractOpBasic> expand(int ignoredExtensionLevel, FunctionExecutor ignoredProgram)
    {
        return List.of(this);
    }

    @Override
    public List<AbstractOpBasic> expand(int ignoredExtensionLevel, FunctionExecutor ignoredProgram, VariableImpl Papa) {
        return expand(ignoredExtensionLevel,ignoredProgram);
    }
    //implementation of deep clone
    @Override
    public AbstractOpBasic myClone() {
        return new OpIncrease(getVariable().myClone(), getLabel().myClone());
    }
    @Override
    public String getRepresentation() {
        return String.format("%s ← %s + 1", getVariable().getRepresentation(), getVariable().getRepresentation()) ;
    }
}
