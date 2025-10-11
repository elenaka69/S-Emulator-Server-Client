package server.engine.impl.api.synthetic;

import server.engine.impl.api.basic.*;
import server.engine.impl.api.skeleton.*;
import server.engine.label.*;
import server.engine.program.FunctionExecutor;
import server.engine.variable.VariableImpl;

import java.util.ArrayList;
import java.util.List;

public class OpJumpEqualVariable extends AbstractOpBasic implements LabelJumper, VariableUser {

    VariableImpl comparableVariable;
    Label jEConstantLabel;

    public OpJumpEqualVariable(VariableImpl variable, Label jEConstantLabel, VariableImpl comparableVariable) {
        super(OpData.JUMP_EQUAL_VARIABLE, variable);
        this.jEConstantLabel = jEConstantLabel;
        this.comparableVariable = comparableVariable;
        generateUniqId();
    }
    public OpJumpEqualVariable(VariableImpl variable, Label jEConstantLabel, VariableImpl comparableVariable, AbstractOpBasic parent) {
        super(OpData.JUMP_EQUAL_VARIABLE, variable, FixedLabel.EMPTY, parent);
        this.jEConstantLabel = jEConstantLabel;
        this.comparableVariable = comparableVariable;
        generateUniqId();
    }

    public OpJumpEqualVariable(VariableImpl variable, Label label, Label jEConstantLabel, VariableImpl comparableVariable) {
        super(OpData.JUMP_EQUAL_VARIABLE, variable, label);
        this.jEConstantLabel = jEConstantLabel;
        this.comparableVariable = comparableVariable;
        generateUniqId();
    }

    public OpJumpEqualVariable(VariableImpl variable, Label label, Label jEConstantLabel, VariableImpl comparableVariable, AbstractOpBasic parent) {
        super(OpData.JUMP_EQUAL_VARIABLE, variable, label, parent);
        this.jEConstantLabel = jEConstantLabel;
        this.comparableVariable = comparableVariable;
        generateUniqId();
    }


    @Override
    public Label execute(FunctionExecutor program) {
        program.increaseCycleCounter(getCycles());
        return program.getVariableValue(getVariable()).equals(program.getVariableValue(comparableVariable)) ? jEConstantLabel : FixedLabel.EMPTY;
    }

    //implementation of deep clone
    @Override
    public AbstractOpBasic myClone() {
        return new OpJumpEqualVariable(getVariable().myClone(), getLabel().myClone(),
                jEConstantLabel.myClone(), comparableVariable.myClone());
    }
    @Override
    public List<AbstractOpBasic> expand(int extensionLevel, FunctionExecutor program) {
        return  expand(extensionLevel,program,getVariable());
    }

    @Override
    public List<AbstractOpBasic> expand(int extensionLevel, FunctionExecutor program, VariableImpl Papa) {
        List<AbstractOpBasic> ops = new ArrayList<>();
        Label lStart    = program.newUniqueLabel();
        Label lCheckZ2  = program.newUniqueLabel();
        Label lNotEqual = program.newUniqueLabel();
        VariableImpl z1 = program.newWorkVar();
        VariableImpl z2 = program.newWorkVar();
        Label targetLabel = jEConstantLabel;
        VariableImpl v    = this.getVariable();
        VariableImpl vTag = this.comparableVariable;
        switch (extensionLevel) {
            case 0: {
                return List.of(this);
            }
            case 1: {
                AbstractOpBasic a1 = new OpAssignment(z1, getLabel(), v,this);
                if (getLabel() != null && !getLabel().equals(FixedLabel.EMPTY)) {
                    program.addLabel(getLabel(), a1);
                }
                AbstractOpBasic a2 = new OpAssignment(z2, vTag,this);

                AbstractOpBasic anchorStart = new OpNeutral(z2,lStart,this);
                program.addLabel(lStart, anchorStart);

                AbstractOpBasic jz1 = new OpJumpZero(z1, lCheckZ2,this);
                AbstractOpBasic jz2 = new OpJumpZero(z2, lNotEqual,this);

                AbstractOpBasic d1 = new OpDecrease(z1,this);
                AbstractOpBasic d2 = new OpDecrease(z2,this);

                AbstractOpBasic goStart = new OpGoToLabel(program.newWorkVar(), lStart,this); //loop until someone is 0

                AbstractOpBasic anchorCheck = new OpNeutral(v,lCheckZ2,this);
                program.addLabel(lCheckZ2, anchorCheck);

                AbstractOpBasic jzEqual = new OpJumpZero(z2, targetLabel,this);

                AbstractOpBasic anchorNotEq = new OpNeutral(v,lNotEqual,this);
                program.addLabel(lNotEqual, anchorNotEq);

                ops.add(a1);
                ops.add(a2);
                ops.add(anchorStart);
                ops.add(jz1);
                ops.add(jz2);
                ops.add(d1);
                ops.add(d2);
                ops.add(goStart);
                ops.add(anchorCheck);
                ops.add(jzEqual);
                ops.add(anchorNotEq);
                break;

            }

            default: {
                AbstractOpBasic a1 = new OpAssignment(z1, getLabel(), v,this);
                if (getLabel() != null && !getLabel().equals(FixedLabel.EMPTY)) {
                    program.addLabel(getLabel(), a1);
                }
                List<AbstractOpBasic> assign1 = a1.expand(extensionLevel-1, program);

                AbstractOpBasic a2 = new OpAssignment(z2, vTag,this);
                List<AbstractOpBasic> assign2 = a2.expand(extensionLevel-1, program);

                AbstractOpBasic anchorStart = new OpNeutral(v,lStart, this);
                program.addLabel(lStart, anchorStart);

                AbstractOpBasic jz1 = new OpJumpZero(z1, lCheckZ2,this);
                List<AbstractOpBasic> jz1ext = jz1.expand(extensionLevel-1, program);

                AbstractOpBasic jz2 = new OpJumpZero(z2, lNotEqual,this);
                List<AbstractOpBasic> jz2ext = jz2.expand(1, program);

                AbstractOpBasic d1 = new OpDecrease(z1,this);
                AbstractOpBasic d2 = new OpDecrease(z2,this);

                AbstractOpBasic goStart = new OpGoToLabel(program.newWorkVar(), lStart,this);
                List<AbstractOpBasic> goExt = goStart.expand(extensionLevel-1, program);

                AbstractOpBasic anchorCheck = new OpNeutral(v,lCheckZ2,this);
                program.addLabel(lCheckZ2, anchorCheck);

                AbstractOpBasic jzEqual = new OpJumpZero(z2, targetLabel,this);
                List<AbstractOpBasic> jzEqExt = jzEqual.expand(1, program);

                AbstractOpBasic anchorNotEq = new OpNeutral(z2,lNotEqual,this);
                program.addLabel(lNotEqual, anchorNotEq);

                ops.addAll(assign1);
                ops.addAll(assign2);
                ops.add(anchorStart);
                ops.addAll(jz1ext);
                ops.addAll(jz2ext);
                ops.add(d1);
                ops.add(d2);
                ops.addAll(goExt);
                ops.add(anchorCheck);
                ops.addAll(jzEqExt);
                ops.add(anchorNotEq);
                break;
            }
        }
        return ops;
    }



    @Override
    public String getRepresentation()
    {
        return String.format("if %s = %s GOTO %s", getVariable().getRepresentation(), comparableVariable.getRepresentation(), jEConstantLabel.getLabelRepresentation());
    }

    @Override
    public Label getJumpLabel() {
        return jEConstantLabel;
    }
    public void setJumpLabel(Label label) {
        jEConstantLabel = label;
    }

    @Override
    public VariableImpl getSecondaryVariable() {
        return comparableVariable;
    }

    @Override
    public void setSecondaryVariable(VariableImpl variable) {
        comparableVariable = variable;
    }
}
