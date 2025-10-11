package server.engine.impl.api.synthetic;

import server.engine.impl.api.basic.*;
import server.engine.impl.api.skeleton.*;
import server.engine.label.*;
import server.engine.program.FunctionExecutor;
import server.engine.variable.VariableImpl;

import java.util.ArrayList;
import java.util.List;

public class OpConstantAssigment extends AbstractOpBasic  {
    Long constant;
    public OpConstantAssigment(VariableImpl variable, Long constant) {
        super(OpData.CONSTANT_ASSIGNMENT, variable);
        this.constant = constant;
        generateUniqId();
    }
    public OpConstantAssigment( VariableImpl variable, Long constant, AbstractOpBasic parent) {
        super(OpData.CONSTANT_ASSIGNMENT, variable, FixedLabel.EMPTY, parent);
        this.constant = constant;
        generateUniqId();
    }

    public OpConstantAssigment(VariableImpl variable, Label label,  Long constant) {
        super(OpData.CONSTANT_ASSIGNMENT, variable, label);
        this.constant = constant;
        generateUniqId();
    }

    public OpConstantAssigment( VariableImpl variable, Label label,  Long constant, AbstractOpBasic parent) {
        super(OpData.CONSTANT_ASSIGNMENT, variable, label, parent);
        this.constant = constant;
        generateUniqId();
    }

    @Override
    public Label execute(FunctionExecutor program) {
        ArrayList<VariableImpl> vars = new ArrayList<>();
        ArrayList<Long> vals = new ArrayList<>();
        vars.add(getVariable());
        vals.add(constant);
        program.addSnap(vars,vals);
        program.increaseCycleCounter(getCycles());

        return FixedLabel.EMPTY;
    }
    //implementation of deep clone
    @Override
    public AbstractOpBasic myClone() {
        return new OpConstantAssigment(getVariable().myClone(), getLabel().myClone(), constant);
    }
    @Override
    public String getRepresentation() {
        return String.format("%s ← %d", getVariable().getRepresentation(), constant);
    }

    @Override
    public List<AbstractOpBasic> expand(int extensionLevel, FunctionExecutor program) {
        return  expand(extensionLevel,program,getVariable());
    }

    @Override
    public List<AbstractOpBasic> expand(int extensionLevel, FunctionExecutor program, VariableImpl Papa) {
        List<AbstractOpBasic> ops = new ArrayList<>();
        VariableImpl variable = getVariable();
        long k = constant; // the constant to assign

        switch (extensionLevel) {
            case 0: {
                return List.of(this);
            }
            case 1: {
                VariableImpl z1 = program.newWorkVar();
                Label loop = program.newUniqueLabel();
                Label after = program.newUniqueLabel();

                AbstractOpBasic zeroV = new OpZeroVariable(variable, getLabel(),this);
                if (getLabel() != null && !getLabel().equals(FixedLabel.EMPTY)) {
                    program.addLabel(getLabel(), zeroV);
                }
                ops.add(zeroV);

                for (long i = 0; i < k; i++) {
                    ops.add(new OpIncrease(z1,this));
                }

                AbstractOpBasic jz = new OpJumpZero(z1, after,this);
                ops.add(jz);
                // z1->v loop
                AbstractOpBasic decZ1 = new OpDecrease(z1, loop,this);
                program.addLabel(loop, decZ1);
                ops.add(decZ1);

                ops.add(new OpIncrease(variable,this));
                ops.add(new OpJumpNotZero(z1, loop,this));
                //end of loop
                AbstractOpBasic end = new OpNeutral(variable, after,this);
                program.addLabel(after, end);
                ops.add(end);

                return ops;
            }

            default: { // 2 or higher
                VariableImpl z1  = program.newWorkVar();
                Label loop = program.newUniqueLabel();
                Label after = program.newUniqueLabel();

                AbstractOpBasic zeroV = new OpZeroVariable(variable, getLabel(),this);
                if (getLabel() != null && !getLabel().equals(FixedLabel.EMPTY)) {
                    program.addLabel(getLabel(), zeroV);
                }
                ops.addAll(zeroV.expand(extensionLevel - 1, program));

                for (long i = 0; i < k; i++) {
                    ops.add(new OpIncrease(z1,this));
                }

                AbstractOpBasic jz = new OpJumpZero(z1, after, this);
                ops.addAll(jz.expand(extensionLevel - 1, program));

                AbstractOpBasic decZ1 = new OpDecrease(z1, loop,this);
                program.addLabel(loop, decZ1);
                ops.add(decZ1);

                ops.add(new OpIncrease(variable,this));
                ops.add(new OpJumpNotZero(z1, loop,this)); // בסיסי

                AbstractOpBasic end = new OpNeutral(variable, after,this);
                program.addLabel(after, end);
                ops.add(end);

                return ops;
            }
        }
    }
}

