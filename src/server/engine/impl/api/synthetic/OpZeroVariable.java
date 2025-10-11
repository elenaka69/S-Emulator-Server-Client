package server.engine.impl.api.synthetic;

import server.engine.impl.api.basic.*;
import server.engine.impl.api.skeleton.*;
import server.engine.label.*;
import server.engine.program.FunctionExecutor;
import server.engine.variable.VariableImpl;

import java.util.ArrayList;
import java.util.List;

public class OpZeroVariable extends AbstractOpBasic  {
    public OpZeroVariable( VariableImpl variable) {
        this(variable, FixedLabel.EMPTY);
    }
    public OpZeroVariable ( VariableImpl variable, AbstractOpBasic parent) {
        super(OpData.ZERO_VARIABLE, variable, FixedLabel.EMPTY, parent);
        generateUniqId();
    }

    public OpZeroVariable(VariableImpl variable, Label label) {
        this( variable, label,null);
    }
    
    public OpZeroVariable( VariableImpl variable, Label label, AbstractOpBasic parent) {
        super(OpData.ZERO_VARIABLE, variable, label, parent);
        generateUniqId();
    }

    @Override
    public Label execute(FunctionExecutor program)
    {
        ArrayList<VariableImpl> vars = new ArrayList<>();
        ArrayList<Long> vals = new ArrayList<>();
        vars.add(getVariable());
        vals.add(0L);
        program.addSnap(vars,vals);
        program.increaseCycleCounter(getCycles());
        return FixedLabel.EMPTY;
    }
    //implementation of deep clone
    @Override
    public AbstractOpBasic myClone() {
        return new OpZeroVariable(getVariable().myClone(), getLabel().myClone());
    }
    @Override
    public List<AbstractOpBasic> expand(int extensionLevel, FunctionExecutor program) {
        return  expand(extensionLevel,program,getVariable());
    }
    @Override
    public List<AbstractOpBasic> expand(int extensionLevel, FunctionExecutor program, VariableImpl Papa) {
        switch (extensionLevel) {
            case 0 : {
                return List.of(this);
            }
            default : {
                // as long as  (var != 0) -> var-- loop
                List<AbstractOpBasic> ops = new ArrayList<>();

                final VariableImpl v = getVariable();

                final Label lBody = program.newUniqueLabel();
                final Label lEnd  = program.newUniqueLabel();

                // אם v != 0 → קפוץ לגוף; אחרת נפילה ל-NEXT (סיום)
                AbstractOpBasic jnz =new OpJumpNotZero(v, getLabel(),lBody,this);
                ops.add(jnz);
                if(getLabel() != null && getLabel() != FixedLabel.EMPTY) {
                    program.addLabel(getLabel(), jnz);
                }
                VariableImpl dummy = program.newWorkVar();
                OpIncrease inc = new OpIncrease(dummy,this);
                ops.add(inc); // פעולה ניטרלית לצורך ספירת מחזורים
                AbstractOpBasic jnzToEnd = new OpJumpNotZero(dummy, lEnd,this);
                ops.add(jnzToEnd);
                // גוף הלולאה
                AbstractOpBasic dec = new OpDecrease(v,lBody,this);
                program.addLabel(lBody, dec);
                ops.add(dec);
                ops.add(new OpJumpNotZero(v, lBody,this));

                AbstractOpBasic endAnchor = new OpNeutral(v, lEnd,this);
                program.addLabel(lEnd, endAnchor);
                ops.add(endAnchor);

                return ops;
            }
        }
    }

    @Override
    public String getRepresentation() {
        return String.format("%s ← 0", getVariable().getRepresentation()) ;
    }
}
