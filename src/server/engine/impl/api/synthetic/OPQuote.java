package server.engine.impl.api.synthetic;

import server.engine.execution.ProgramCollection;
import server.engine.impl.api.basic.*;
import server.engine.impl.api.skeleton.*;
import server.engine.impl.api.synthetic.OpAssignment;
import server.engine.impl.api.synthetic.OpFunctionBase;
import server.engine.label.*;
import server.engine.program.FunctionExecutor;
import server.engine.program.FunctionExecutorImpl;
import server.engine.variable.VariableImpl;
import java.util.ArrayList;
import java.util.List;

public class OPQuote extends OpFunctionBase {

    public OPQuote(VariableImpl variable, String functionName, String functionArguments) {
        this(variable, FixedLabel.EMPTY, functionName, functionArguments,null);
    }

    public OPQuote( VariableImpl variable, String functionName, String functionArguments, AbstractOpBasic parent) {
        this(variable, FixedLabel.EMPTY, functionName, functionArguments, parent);
    }

    public OPQuote(VariableImpl variable, Label label,  String functionName, String functionArguments) {
        this(variable, label, functionName, functionArguments, null);
    }

    public OPQuote(VariableImpl variable, Label label, String functionName, String functionArguments,  AbstractOpBasic parent) {
        super(OpData.QUOTE, variable, label, functionName, functionArguments, parent);
        generateUniqId();
    }

    public OPQuote(OPQuote src)
    {
        super(OpData.QUOTE, src.getVariable().myClone(), src.getLabel().myClone(), src.getFunctionName(), src.getStrFunctionArguments(), null);
        generateUniqId();
    }

    @Override
    public Label execute(FunctionExecutor program) { // not supported for this OP
        return FixedLabel.EMPTY;
    }

    @Override
    public Label execute(FunctionExecutor program, List <FunctionExecutor> functions) {

        if (function == null) {
            System.out.println("NULL");
        }
        ((FunctionExecutorImpl)function).run(program, functionArguments, functions);
        Long result = function.getVariableValue(VariableImpl.RESULT);
        ArrayList<VariableImpl> vars = new ArrayList<>();
        ArrayList<Long> vals = new ArrayList<>();
        vars.add(getVariable());
        vals.add(result);

        program.addSnap(vars,vals);
        program.increaseCycleCounter(getCycles());

        return FixedLabel.EMPTY;
    }

    @Override
    protected AbstractOpBasic getFinalOp(VariableImpl resultVar, AbstractOpBasic parent) {
        return  new OpAssignment(getVariable(), resultVar, parent);
    }

    @Override
    public List<AbstractOpBasic> expand(int ignoredExtensionLevel, FunctionExecutor ignoredProgram, VariableImpl Papa) {
        return expand(ignoredExtensionLevel, ignoredProgram);
    }

    @Override
    public AbstractOpBasic myClone() {
        return new OPQuote(this);
    }

    @Override
    public String getRepresentation() {
        String userString = ProgramCollection.getFunction(functionName).getUserString();
        return String.format("%s ← %s", getVariable().getRepresentation(), userString);
    }
}
