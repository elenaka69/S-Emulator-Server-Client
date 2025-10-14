package server.engine.impl.api.skeleton;

import server.engine.program.FunctionExecutor;
import server.engine.variable.VariableImpl;
import server.engine.label.*;

import java.util.List;

public abstract class AbstractOpBasic  {

    protected OpData opData;
    private Label label;
    private VariableImpl variable;
    private AbstractOpBasic parent;
    private String uniqId;

    private int depth;

    //Ctors
    protected AbstractOpBasic(OpData opData, VariableImpl variable) { //allow to create without label
        this(opData, variable, FixedLabel.EMPTY, null);
    }
    protected AbstractOpBasic(OpData opData, VariableImpl variable, AbstractOpBasic parent) { //allow to create without label
        this(opData, variable, FixedLabel.EMPTY,parent);
    }
    protected AbstractOpBasic(OpData opData, VariableImpl variable, Label label)
    {
        this(opData, variable, label, null);
    }

    protected AbstractOpBasic(OpData opData, VariableImpl variable, Label label, AbstractOpBasic parent) {
        this.opData = opData;
        this.label = label;
        this.variable = variable;
        this.parent = parent;
    }

    public int calculateDepth() {
        int depth = 0;
        AbstractOpBasic currParent = this.parent;
        while (currParent != null) {
            depth++;
            currParent = currParent.getParent();
        }
        this.depth = depth;
        return depth;
    }

    public int getDepth() {
        return depth;
    }


    protected void generateUniqId()
    {
        StringBuilder sb = new StringBuilder();
        String lbl;

        lbl = label == null ? "" : label.getLabelRepresentation();
        if (parent != null) {
            sb.append(parent.getUniqId());
            sb.append("<<< ");
        }

        sb.append(String.format("(%S)[%5s] %S (%d)  ",getType(), lbl, getRepresentation(), getCycles()));
        uniqId = sb.toString();
    }

    public String getUniqId() {
        return uniqId;
    }

    public VariableImpl getVariable() {
        return variable;
    }

    public String getType() {
        return opData.getType().equals(OpType.BASIC)? "B" : "S";
    }
    public String getArch() {
        switch (opData.getArch()) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            default: return "Unknown";
        }
    }

    public Label getLabel() {
        return label;
    }

    public String getName() {
        return opData.getName();
    }

    public int getDegree() {
        return opData.getDegree();
    }

    public int getCycles() {
        return opData.getCycles();
    }

    public AbstractOpBasic getParent() { return parent;};

    public String toString() {
        return getName();
    }

    public abstract Label execute(FunctionExecutor program);

    public abstract List<AbstractOpBasic> expand(int extensionLevel, FunctionExecutor program);

    public abstract List<AbstractOpBasic> expand(int ignoredExtensionLevel, FunctionExecutor ignoredProgram, VariableImpl Papa);
    //implement a deep clone method
    public abstract AbstractOpBasic myClone();

    public String getRepresentation() {
        return " ";
    }

    public void setLabel(Label label) {
        this.label = label;
    }
    public void setVariable (VariableImpl variable) {
        this.variable = variable;
    }

    public void setParent(AbstractOpBasic parent) {
        this.parent = parent;
    }

    public int getCredit() { return opData.getCredit(); }
}
