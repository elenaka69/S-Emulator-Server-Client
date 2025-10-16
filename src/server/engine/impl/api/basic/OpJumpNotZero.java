package server.engine.impl.api.basic;

import server.engine.impl.api.skeleton.AbstractOpBasic;
import server.engine.impl.api.skeleton.LabelJumper;
import server.engine.impl.api.skeleton.OpData;
import server.engine.label.*;
import server.engine.program.FunctionExecutor;
import server.engine.variable.VariableImpl;

import java.util.List;

public class OpJumpNotZero extends AbstractOpBasic implements LabelJumper {
    private Label jnzLabel;

    public OpJumpNotZero(VariableImpl variable, Label jnzLabel) {
        this(variable, FixedLabel.EMPTY, jnzLabel,null);
    }

    public OpJumpNotZero(VariableImpl variable, Label jnzLabel, AbstractOpBasic parent) {
        this(variable, FixedLabel.EMPTY, jnzLabel, parent);
    }

    public OpJumpNotZero(VariableImpl variable, Label jnzLabel, Label label) {
        this( variable, label, jnzLabel ,null);
    }

    public OpJumpNotZero(VariableImpl variable, Label label, Label jnzLabel, AbstractOpBasic parent) {
        super(OpData.JUMP_NOT_ZERO, variable, label, parent);
        this.jnzLabel = jnzLabel;
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
        Long variableValue = program.getVariableValue(getVariable());
        program.increaseCycleCounter(getCycles());
        if (variableValue != 0) {
            return jnzLabel;
        }
        return FixedLabel.EMPTY;
    }

    //implementation of deep clone
    @Override
    public AbstractOpBasic myClone() {
        return  new OpJumpNotZero(getVariable().myClone(), jnzLabel.myClone(), getLabel().myClone());
    }

    @Override
    public String getRepresentation() {
        return String.format("IF %s!=0 GOTO %s", getVariable().getRepresentation(), jnzLabel.getLabelRepresentation()) ;
    }

    @Override
    public Label getJumpLabel() {
        return  jnzLabel;
    }

    @Override
    public void setJumpLabel(Label label) {
        jnzLabel = label;
    }
}
