package server.engine.impl.api.synthetic;

import server.engine.impl.api.basic.*;
import server.engine.impl.api.skeleton.*;
import server.engine.label.*;
import server.engine.program.FunctionExecutor;
import server.engine.program.FunctionExecutorImpl;
import server.engine.variable.VariableImpl;

import java.util.List;

public class OPJumpEqualFunction extends OpFunctionBase implements LabelJumper {
    private  Label JEFunctionLabel;

    public OPJumpEqualFunction(VariableImpl variableImpl, String functionName, String functionArguments, Label jEFunctionLabel, FunctionExecutor function) {
        this(variableImpl, FixedLabel.EMPTY, functionName, functionArguments, jEFunctionLabel, function,  null);
    }
    public OPJumpEqualFunction(VariableImpl variableImpl, String functionName, String functionArguments, Label jEFunctionLabel, FunctionExecutor function, AbstractOpBasic parent) {
        this(variableImpl, FixedLabel.EMPTY, functionName, functionArguments, jEFunctionLabel, function, parent);
    }

    public OPJumpEqualFunction(VariableImpl variableImpl, Label label, String functionName, String functionArguments, Label jEFunctionLabel, FunctionExecutor function) {
        this(variableImpl, label, functionName, functionArguments, jEFunctionLabel, function, null);
    }

    public OPJumpEqualFunction(VariableImpl variableImpl, Label label, String functionName, String functionArguments, Label JEFunctionLabel, FunctionExecutor function, AbstractOpBasic parent) {
        super(OpData.JUMP_EQUAL_FUNCTION, variableImpl, label, functionName, functionArguments, function, parent);
        this.JEFunctionLabel = JEFunctionLabel;
        generateUniqId();
    }
    public OPJumpEqualFunction(OPJumpEqualFunction src)
    {
        super(OpData.JUMP_EQUAL_FUNCTION, src.getVariable(), src.getLabel(), src.getFunctionName(), src.getStrFunctionArguments(), src.getFunction().myClone(), null);
        this.JEFunctionLabel = src.getJEFunctionLabel();
        generateUniqId();
    }

    public Label getJEFunctionLabel() {
        return JEFunctionLabel;
    }

    @Override
    public Label execute(FunctionExecutor program) { // not supported for this OP

        return FixedLabel.EMPTY;
    }

    @Override
    public Label execute(FunctionExecutor program, List <FunctionExecutor> functions) {
        Long result = ((FunctionExecutorImpl)function).run(program, functionArguments, functions);
        if (program.getVariableValue(getVariable()).equals(result))
            return JEFunctionLabel;

        return FixedLabel.EMPTY;
    }

    @Override
    protected AbstractOpBasic getFinalOp(VariableImpl resultVar, AbstractOpBasic parent) {
        return new OpJumpEqualVariable( getVariable(), JEFunctionLabel, resultVar, parent );
    }

    @Override
    public List<AbstractOpBasic> expand(int ignoredExtensionLevel, FunctionExecutor ignoredProgram, VariableImpl Papa) {
        return expand(ignoredExtensionLevel, ignoredProgram);
    }

    @Override
    public AbstractOpBasic myClone() {
         return new OPJumpEqualFunction(this);
    }

    @Override
    public String getRepresentation() {
        return String.format("if %s = %s GOTO %s", getVariable().getRepresentation(), ((FunctionExecutorImpl)function).getUserString(), JEFunctionLabel.getLabelRepresentation());
    }

    @Override
    public Label getJumpLabel() {
        return JEFunctionLabel;
    }
    public void setJumpLabel(Label label) {
        JEFunctionLabel = label;
    }
}
