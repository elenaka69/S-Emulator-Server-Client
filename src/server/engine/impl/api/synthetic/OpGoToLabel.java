package server.engine.impl.api.synthetic;

import server.engine.impl.api.basic.OpIncrease;
import server.engine.impl.api.basic.OpJumpNotZero;
import server.engine.impl.api.skeleton.*;
import server.engine.label.*;
import server.engine.program.FunctionExecutor;
import server.engine.variable.VariableImpl;

import java.util.ArrayList;
import java.util.List;

public class OpGoToLabel extends AbstractOpBasic implements LabelJumper {
    Label nextLabel;
    public OpGoToLabel(VariableImpl variable, Label nextLabel) {
        super(OpData.GOTO_LABEL,variable);
        this.nextLabel = nextLabel;
        generateUniqId();
    }
    public OpGoToLabel(VariableImpl variable, Label nextLabel, AbstractOpBasic parent) {
        super(OpData.GOTO_LABEL, variable, FixedLabel.EMPTY, parent);
        this.nextLabel = nextLabel;
        generateUniqId();

    }

    public OpGoToLabel(VariableImpl variable, Label label,Label nextLabel) {
        super(OpData.GOTO_LABEL, variable, label);
        this.nextLabel = nextLabel;
        generateUniqId();
    }

    public OpGoToLabel( VariableImpl variable, Label label, Label nextLabel, AbstractOpBasic parent) {
        super(OpData.GOTO_LABEL, variable, label, parent);
        this.nextLabel = nextLabel;
        generateUniqId();
    }

    @Override
    public Label execute(FunctionExecutor program) {
        program.increaseCycleCounter(getCycles());
        return nextLabel;
    }
    //implementation of deep clone
    @Override
    public AbstractOpBasic myClone() {
        AbstractOpBasic op;
        if(nextLabel.equals(FixedLabel.EXIT))
            op= new OpGoToLabel(getVariable().myClone(), getLabel().myClone(), FixedLabel.EXIT);
        else if(nextLabel.equals(FixedLabel.EMPTY))
            op = new OpGoToLabel(getVariable().myClone(), getLabel().myClone(), FixedLabel.EMPTY);
        else
              op = new OpGoToLabel(getVariable().myClone(), getLabel().myClone(), nextLabel.myClone());
        return op;

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
            default: {
                VariableImpl tmp = program.newWorkVar();
                AbstractOpBasic inc = new OpIncrease(tmp, getLabel(),this);
                if (getLabel() != null && getLabel() != FixedLabel.EMPTY) {
                    program.addLabel(getLabel(), inc);
                }

                Label target = nextLabel;
                AbstractOpBasic jnz = new OpJumpNotZero(tmp, target,this);

                ops.add(inc);
                ops.add(jnz);
                return ops;
            }
        }
    }


    @Override
    public String getRepresentation()
    {
        return String.format("GOTO %s", nextLabel.getLabelRepresentation()) ;
    }

    public List<AbstractOpBasic> expand() {
        List<AbstractOpBasic> expanded = new ArrayList<>();
        // 1. Increase dummy variable by 1 (use the synthetic’s own label for this instruction)
        expanded.add(new OpIncrease(getVariable().myClone(), getLabel().myClone()));
        // 2. JumpNotZero to the target label (unconditional jump since var != 0 after increase)
        expanded.add(new OpJumpNotZero(getVariable().myClone(), nextLabel.myClone()));
        return expanded;
    }

    @Override
    public Label getJumpLabel() {
        return nextLabel;
    }
    public void setJumpLabel(Label label) {
        nextLabel = label;
    }
}
