package server.engine.impl.api.synthetic;

import server.engine.impl.api.basic.*;
import server.engine.impl.api.skeleton.*;
import server.engine.label.*;
import server.engine.program.FunctionExecutor;
import server.engine.variable.VariableImpl;

import java.util.ArrayList;
import java.util.List;

public class OpJumpEqualConstant extends AbstractOpBasic implements LabelJumper {
    Label jEConstantLabel;
    Long constant;
    public OpJumpEqualConstant(VariableImpl variable, Label jEConstantLabel, Long constantValue) {
        super(OpData.JUMP_EQUAL_CONSTANT, variable);
        this.jEConstantLabel = jEConstantLabel;
        this.constant = constantValue;
        generateUniqId();
    }
    public OpJumpEqualConstant(VariableImpl variable, Label jEConstantLabel, Long constantValue, AbstractOpBasic parent) {
        super(OpData.JUMP_EQUAL_CONSTANT, variable, FixedLabel.EMPTY, parent);
        this.jEConstantLabel = jEConstantLabel;
        this.constant = constantValue;
        generateUniqId();
    }

    public OpJumpEqualConstant(VariableImpl variable, Label label, Label jEConstantLabel, Long constantValue) {
        super(OpData.JUMP_EQUAL_CONSTANT, variable, label);
        this.jEConstantLabel = jEConstantLabel;
        this.constant = constantValue;
        generateUniqId();
    }

    public OpJumpEqualConstant(VariableImpl variable, Label label, Label jEConstantLabel, Long constantValue, AbstractOpBasic parent) {
        super(OpData.JUMP_EQUAL_CONSTANT, variable, label, parent);
        this.jEConstantLabel = jEConstantLabel;
        this.constant = constantValue;
        generateUniqId();
    }

    @Override
    public Label execute(FunctionExecutor program) {
        program.increaseCycleCounter(getCycles());
        return program.getVariableValue(getVariable()).equals(constant) ? jEConstantLabel : FixedLabel.EMPTY;
    }
    //implementation of deep clone
    @Override
    public AbstractOpBasic myClone() {
        return new OpJumpEqualConstant(getVariable().myClone(), getLabel().myClone(), jEConstantLabel.myClone(), constant);
    }
    @Override
    public List<AbstractOpBasic> expand(int extensionLevel, FunctionExecutor program) {
        return  expand(extensionLevel,program,getVariable());
    }

    @Override
    public List<AbstractOpBasic> expand(int extensionLevel, FunctionExecutor program, VariableImpl Papa) {
        List<AbstractOpBasic> ops = new ArrayList<>();
        switch (extensionLevel) {
            case 0: {
                return List.of(this);
            }

            case 1: {
                VariableImpl z1 = program.newWorkVar();
                VariableImpl v = getVariable();
                long k = constant;
                Label target   = jEConstantLabel;
                Label notEqLbl = program.newUniqueLabel();

                // z1 = v
                AbstractOpBasic a1 = new OpAssignment(z1, getLabel(), v,this);
                if (getLabel() != null && !getLabel().equals(FixedLabel.EMPTY)) {
                    program.addLabel(getLabel(), a1);
                }
                ops.add(a1);

                // check if equal
                for (long i = 0; i < k; i++) {
                    ops.add(new OpJumpZero(z1, notEqLbl,this)); //
                    ops.add(new OpDecrease(z1,this));
                }
                // z1 > k?
                ops.add(new OpJumpNotZero(z1, notEqLbl,this));
                ops.add(new OpGoToLabel(z1, target,this)); // great success for me

                // not equal anchor
                AbstractOpBasic end = new OpNeutral(v, notEqLbl,this);
                program.addLabel(notEqLbl, end);
                ops.add(end);
                break;
            }

            case 2: {
                VariableImpl z1 = program.newWorkVar();
                VariableImpl v     = getVariable();
                long k         = constant;
                Label target   = jEConstantLabel;
                Label notEqLbl = program.newUniqueLabel();

                AbstractOpBasic a1 = new OpAssignment(z1, getLabel(), v,this);
                if (getLabel() != null && !getLabel().equals(FixedLabel.EMPTY)) {
                    program.addLabel(getLabel(), a1);
                }
                ops.addAll(a1.expand(1, program));

                for (long i = 0; i < k; i++) {
                    AbstractOpBasic jz = new OpJumpZero(z1, notEqLbl,this);
                    ops.addAll(jz.expand(1, program));
                    ops.add(new OpDecrease(z1,this));
                }

                AbstractOpBasic jnz = new OpJumpNotZero(z1, notEqLbl,this);
                ops.add(jnz);

                AbstractOpBasic go = new OpGoToLabel(program.newWorkVar(), target,this);
                ops.addAll(go.expand(1, program));

                AbstractOpBasic end = new OpNeutral(v, notEqLbl,this);
                program.addLabel(notEqLbl, end);
                ops.add(end);
                break;
            }
            default: {
                VariableImpl z1    = program.newWorkVar();
                VariableImpl v     = getVariable();
                long k  = constant;
                Label target   = this.jEConstantLabel;
                Label notEqLbl = program.newUniqueLabel();

                AbstractOpBasic a1 = new OpAssignment(z1, getLabel(), v,this);
                if (getLabel() != null && !getLabel().equals(FixedLabel.EMPTY)) {
                    program.addLabel(getLabel(), a1);
                }
                ops.addAll(a1.expand(extensionLevel - 1, program));

                for (long i = 0; i < k; i++) {
                    AbstractOpBasic jz = new OpJumpZero(z1, notEqLbl,this);
                    ops.addAll(jz.expand(extensionLevel - 1, program));
                    ops.add(new OpDecrease(z1,this));
                }

                AbstractOpBasic jnz = new OpJumpNotZero(z1, notEqLbl,this);
                ops.add(jnz);

                AbstractOpBasic go = new OpGoToLabel(program.newWorkVar(), target,this);
                ops.addAll(go.expand(extensionLevel - 1, program));

                AbstractOpBasic end = new OpNeutral(v, notEqLbl,this);
                program.addLabel(notEqLbl, end);
                ops.add(end);
                break;
            }
        }
        return ops;
    }


    @Override
    public String getRepresentation() {
        return String.format("if %s = %d GOTO %s", getVariable().getRepresentation(), constant, jEConstantLabel.getLabelRepresentation());
    }

    @Override
    public Label getJumpLabel() {
        return  jEConstantLabel;
    }
    public void setJumpLabel(Label label) {
        jEConstantLabel = label;
    }
}
