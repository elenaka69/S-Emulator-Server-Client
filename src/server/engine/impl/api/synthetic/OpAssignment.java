package server.engine.impl.api.synthetic;

import server.engine.impl.api.basic.*;
import server.engine.impl.api.skeleton.*;
import server.engine.label.*;
import server.engine.program.FunctionExecutor;
import server.engine.variable.VariableImpl;

import java.util.ArrayList;
import java.util.List;

public class OpAssignment extends AbstractOpBasic implements VariableUser {
    VariableImpl outSideVar;
    public OpAssignment( VariableImpl variable, VariableImpl outSideVar) {
        super(OpData.ASSIGNMENT, variable);
        this.outSideVar = outSideVar;
        generateUniqId();
    }
    public OpAssignment( VariableImpl variable, VariableImpl outSideVar, AbstractOpBasic parent) {
        super(OpData.ASSIGNMENT, variable, FixedLabel.EMPTY, parent);
        this.outSideVar = outSideVar;
        generateUniqId();
    }

    public OpAssignment(VariableImpl variable, Label label,  VariableImpl outSideVar) {
        super(OpData.ASSIGNMENT, variable, label);
        this.outSideVar = outSideVar;
        generateUniqId();
    }
    
    public OpAssignment(VariableImpl variable, Label label, VariableImpl outSideVar, AbstractOpBasic parent) {
        super(OpData.ASSIGNMENT, variable, label, parent);
        this.outSideVar = outSideVar;
        generateUniqId();
    }

    @Override
    public Label execute(FunctionExecutor program) {
        Long variableValue = program.getVariableValue(outSideVar);
        ArrayList<VariableImpl> vars = new ArrayList<>();
        ArrayList<Long> vals = new ArrayList<>();
        vars.add(getVariable());
        vals.add(variableValue);
        program.addSnap(vars,vals);
        program.increaseCycleCounter(getCycles());

        return FixedLabel.EMPTY;
    }
    //implementation of deep clone
    @Override
    public AbstractOpBasic myClone() {
        return new OpAssignment(getVariable().myClone(), getLabel().myClone(), outSideVar.myClone());
    }
    @Override
    public String getRepresentation() {
        return String.format("%s ← %s", getVariable().getRepresentation(), outSideVar.getRepresentation()) ;
    }
    @Override
    public List<AbstractOpBasic> expand(int extensionLevel, FunctionExecutor program) {
        return  expand(extensionLevel, program, getVariable());
    }

    @Override
    public List<AbstractOpBasic> expand(int extensionLevel, FunctionExecutor program, VariableImpl Papa) {
        List<AbstractOpBasic> ops = new ArrayList<>();

        switch (extensionLevel) {
            case 0: {
                return List.of(this);
            }
            case 1: {
                Label label1 = program.newUniqueLabel(); //loop externalVar ->z1
                Label label2 = program.newUniqueLabel(); // z1 -> v
                Label label3 = program.newUniqueLabel(); // final

                VariableImpl v    = this.getVariable();   //
                VariableImpl externalVar = this.outSideVar;      //
                VariableImpl z1   = program.newWorkVar(); // dummy accumulator ()

                AbstractOpBasic zeroV = new OpZeroVariable(v, getLabel(), this ); // clean v
                if (getLabel() != null && getLabel() != FixedLabel.EMPTY) {
                    program.addLabel(getLabel(), zeroV);
                }

                AbstractOpBasic jnzVTagEnter = new OpJumpNotZero(externalVar, label1, this);
                AbstractOpBasic gotoEnd = new OpGoToLabel(program.newWorkVar(), label3,this);

                 AbstractOpBasic decVTagL1 = new OpDecrease(externalVar, label1,this);
                program.addLabel(label1, decVTagL1);
                AbstractOpBasic incZ1      = new OpIncrease(z1,this);
                AbstractOpBasic backL1     = new OpJumpNotZero(externalVar, label1,this);

                // increase external var and v together
                AbstractOpBasic decZ1L2 = new OpDecrease(z1, label2,this);
                program.addLabel(label2, decZ1L2);
                AbstractOpBasic incV     = new OpIncrease(v,this);
                AbstractOpBasic incVTag  = new OpIncrease(externalVar,this);
                AbstractOpBasic backL2   = new OpJumpNotZero(z1, label2,this);

                AbstractOpBasic endAnchor = new OpNeutral(v, label3,this);
                program.addLabel(label3, endAnchor);

                ops.add(zeroV);
                ops.add(jnzVTagEnter);
                ops.add(gotoEnd);
                ops.add(decVTagL1);
                ops.add(incZ1);
                ops.add(backL1);
                ops.add(decZ1L2);
                ops.add(incV);
                ops.add(incVTag);
                ops.add(backL2);
                ops.add(endAnchor);
                return ops;
            }

            default: { // 2+
                Label label1 = program.newUniqueLabel(); //loop vTag ->z1
                Label label2 = program.newUniqueLabel(); // z1 -> v
                Label label3 = program.newUniqueLabel(); // final

                VariableImpl v    = this.getVariable();   //
                VariableImpl vTag = this.outSideVar;      //
                VariableImpl z1   = program.newWorkVar(); // dummy accumulator ()

                AbstractOpBasic zeroV = new OpZeroVariable(v, getLabel(),this ); // clean v
                if (getLabel() != null && getLabel() != FixedLabel.EMPTY) {
                    program.addLabel(getLabel(), zeroV);
                }
                ops.addAll(zeroV.expand(1, program));

                // if the external var is 0 go to end
                AbstractOpBasic jnzVTagEnter = new OpJumpNotZero(vTag, label1,this);
                ops.add(jnzVTagEnter);
                AbstractOpBasic gotoEnd = new OpGoToLabel(program.newWorkVar(), label3, this);
                ops.addAll(gotoEnd.expand(1, program));

                //pour external var to z1
                AbstractOpBasic decVTagL1 = new OpDecrease(vTag, label1,this);
                program.addLabel(label1, decVTagL1);
                ops.add(decVTagL1);
                AbstractOpBasic incZ1      = new OpIncrease(z1,this);
                ops.add(incZ1);
                AbstractOpBasic backL1     = new OpJumpNotZero(vTag, label1,this);
                ops.add(backL1);

                // increase external var and v together
                AbstractOpBasic decZ1L2 = new OpDecrease(z1, label2,this);
                ops.add(decZ1L2);
                program.addLabel(label2, decZ1L2);
                AbstractOpBasic incV     = new OpIncrease(v,this);
                ops.add(incV);
                AbstractOpBasic incVTag  = new OpIncrease(vTag,this);
                ops.add(incVTag);
                AbstractOpBasic backL2   = new OpJumpNotZero(z1, label2,this);
                ops.add(backL2);

                AbstractOpBasic endAnchor = new OpNeutral(v, label3,this);
                ops.add(endAnchor);
                program.addLabel(label3, endAnchor);

                return ops;
            }
        }
    }

    @Override
    public void setSecondaryVariable(VariableImpl variable) {
        outSideVar = variable;
    }

    @Override
    public VariableImpl getSecondaryVariable() {
        return outSideVar;
    }
}
