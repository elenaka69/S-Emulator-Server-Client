package server.engine.impl.api.synthetic;

import server.engine.impl.api.basic.*;
import server.engine.impl.api.skeleton.*;
import server.engine.label.*;
import server.engine.program.FunctionExecutor;
import server.engine.variable.VariableImpl;

import java.util.ArrayList;
import java.util.List;

public class OpJumpZero extends AbstractOpBasic implements LabelJumper {
    Label jZLabel;
    public OpJumpZero( VariableImpl variable, Label jZLabel) {
        super(OpData.JUMP_ZERO, variable);
        this.jZLabel = jZLabel;
        generateUniqId();
    }
    public OpJumpZero(VariableImpl variable, Label jZLabel, AbstractOpBasic parent) {
        super(OpData.JUMP_ZERO, variable, FixedLabel.EMPTY, parent);
        this.jZLabel = jZLabel;
        generateUniqId();
    }

    public OpJumpZero( VariableImpl variable, Label label, Label jZLabel) {
        super(OpData.JUMP_ZERO, variable, label);
        this.jZLabel = jZLabel;
        generateUniqId();
    }

    public OpJumpZero(VariableImpl variable, Label label, Label jZLabel, AbstractOpBasic parent) {
        super(OpData.JUMP_ZERO, variable, label, parent);
        this.jZLabel = jZLabel;
        generateUniqId();
    }

    @Override
    public Label execute(FunctionExecutor program) {
        program.increaseCycleCounter(getCycles());
        return program.getVariableValue(getVariable()) == 0L ? jZLabel : FixedLabel.EMPTY;
    }
    //implementation of deep clone
    @Override
    public AbstractOpBasic myClone() {
        return new OpJumpZero(getVariable().myClone(), getLabel().myClone(), jZLabel.myClone());
    }

    @Override
    public List<AbstractOpBasic> expand(int ignoredExtensionLevel, FunctionExecutor ignoredProgram) {
        return expand(ignoredExtensionLevel,ignoredProgram, getVariable());
    }
    @Override
    public List<AbstractOpBasic> expand(int extensionLevel, FunctionExecutor program, VariableImpl Papa) {
        List<AbstractOpBasic> ops = new ArrayList<>();

        switch (extensionLevel) {
            case 0: {
                return List.of(this);
            }
            case 1: {
                Label skip = program.newUniqueLabel();

                AbstractOpBasic jnz = new OpJumpNotZero(getVariable(), getLabel(),skip ,this);
                if (getLabel() != null && getLabel() != FixedLabel.EMPTY) {
                    program.addLabel(getLabel(), jnz);
                }

                VariableImpl dummy = program.newWorkVar();
                AbstractOpBasic go = new OpGoToLabel(dummy, jZLabel,this);

                AbstractOpBasic anchor = new OpNeutral(getVariable(), skip,this);
                program.addLabel(skip, anchor);

                ops.add(jnz);
                ops.add(go);
                ops.add(anchor);
                break;
            }

            default: {
                Label skip = program.newUniqueLabel();

                AbstractOpBasic jnz = new OpJumpNotZero(getVariable(), getLabel(),skip ,this);
                if (getLabel() != null && getLabel() != FixedLabel.EMPTY) {
                    program.addLabel(getLabel(), jnz);
                }
                ops.add(jnz);

                VariableImpl dummy = program.newWorkVar();
                AbstractOpBasic go = new OpGoToLabel(dummy, jZLabel,this);
                ops.addAll(go.expand(1, program));

                AbstractOpBasic anchor = new OpNeutral(getVariable(), skip,this);
                program.addLabel(skip, anchor);
                ops.add(anchor);
                break;

            }
        }
        return ops;
    }

    @Override
    public String getRepresentation() {
        return String.format("if %s = 0 GOTO %s", getVariable().getRepresentation(), jZLabel.getLabelRepresentation()) ;
    }

    @Override
    public Label getJumpLabel() {
        return jZLabel;
    }
    public void setJumpLabel(Label label) {
        jZLabel = label;
    }
}
