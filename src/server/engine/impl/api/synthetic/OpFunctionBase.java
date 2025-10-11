package server.engine.impl.api.synthetic;


import server.engine.execution.EngineManager;
import server.engine.impl.api.basic.OpNeutral;
import server.engine.impl.api.skeleton.AbstractOpBasic;
import server.engine.impl.api.skeleton.OpData;
import server.engine.impl.api.skeleton.functionArgs.AbstractArgument;
import server.engine.impl.api.skeleton.functionArgs.FunctionArgument;
import server.engine.label.*;
import server.engine.program.FunctionExecutor;
import server.engine.variable.VariableImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract  class OpFunctionBase extends AbstractOpBasic {

    protected final String functionName;
    protected List<AbstractArgument> functionArguments;
    protected String strFunctionArguments;
    protected int funcCycles;
    protected FunctionExecutor function;
    private List<FunctionExecutor> historyFunctions;

    public OpFunctionBase(OpData opData, VariableImpl variable, Label label, String functionName, String arguments, FunctionExecutor function, AbstractOpBasic parent) {
        super(opData, variable, label, parent);
        this.functionName = functionName;
        this.strFunctionArguments = arguments;
        this.function = function;
        historyFunctions = new ArrayList<>();
        historyFunctions.add(function);
        try{
            functionArguments = AbstractArgument.parseFunctionArguments(arguments);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private int calcDegree(FunctionExecutor function, AbstractArgument arg) {
        if (arg == null) {
            return 0;
        }

        List<AbstractArgument> args;
        FunctionExecutor subFunction;
        AtomicInteger maxDegree = new AtomicInteger();

        if (arg.getType().equals(AbstractArgument.ArgumentTypes.VARIABLE)) {
            return function.getProgramDegree() + 1;
        } else {
            subFunction = EngineManager.getFunction(((FunctionArgument) arg).getFunctionName());

            args = ((FunctionArgument) arg).getArgument();
            if (args == null) {
                return subFunction.getProgramDegree() + 1;
            }
            args.forEach(subArg-> {

                int degree = calcDegree(subFunction,subArg);
                if (degree > maxDegree.get()) {
                    maxDegree.set(degree);
                }
            });
            return maxDegree.get() + 1;
        }
    }

    private int calculateMaxDegree(FunctionExecutor function, List<AbstractArgument> args) {

        if (args == null || args.isEmpty()) {
            return function.getProgramDegree() + 1;
        }

        AtomicInteger maxDegree = new AtomicInteger();
        args.forEach(arg-> {
            int degree = calcDegree(function, arg);
            if (degree > maxDegree.get()) {
                maxDegree.set(degree);
            }
        });
        return maxDegree.get();
    }
    public void setDegree()
    {
        int degree = calculateMaxDegree(function, functionArguments);
        opData.setDegree(degree);
    }


    public String getStrFunctionArguments() {
        return strFunctionArguments;
    }

    public FunctionExecutor getFunction() {
        return function;
    }

    protected  List<Long> convertVarToValue(FunctionExecutor program)
    {
        List<Long> funcVars = new ArrayList<>();
     /*   variables.forEach( funcVar-> {
            funcVars.add(program.getVariableValue(funcVar));
        });*/
        return funcVars;
    }

    @Override
    public abstract Label execute(FunctionExecutor program);

    public abstract Label execute(FunctionExecutor program, List <FunctionExecutor> functions);

    protected abstract AbstractOpBasic getFinalOp(VariableImpl resultVar, AbstractOpBasic parent);

    private List<AbstractOpBasic> expanding(int expansionLevel, FunctionExecutor program)
    {
        VariableImpl resultVar = program.newWorkVar();
        List<AbstractOpBasic> ops = new ArrayList<>(expandFunction(function, resultVar, this));

        AbstractOpBasic finalOp = getFinalOp(resultVar, this);
        ops.add(finalOp);

        reAssignLabels(ops, program);

        return ops;
    }

    @Override
    public List<AbstractOpBasic> expand(int expansionLevel, FunctionExecutor program) {
        List<AbstractOpBasic> ops = new ArrayList<>();

        switch (expansionLevel) {
            case 0: {
                return List.of(this);
            }
            default:
                return expanding(expansionLevel, program);
        }
    }

    protected void reAssignLabels(List<AbstractOpBasic> ops, FunctionExecutor func)
    {
         // TODO : fix MainController
        /*
        Map<Label, Label> labelMapOldNew = new HashMap<>();
        boolean containsExit = false;
        Label currentLabel;
        Label ExitLabel = null;

        MainController controller = MainApp.getMainController();
        SprogramImpl mainProgram = (SprogramImpl) controller.getMainProgram();

        for (AbstractOpBasic op: ops) {
            currentLabel = op.getLabel();
            if(currentLabel != null && !currentLabel.equals(FixedLabel.EMPTY) && !currentLabel.equals(FixedLabel.EXIT) )
            {
                if(labelMapOldNew.containsKey(currentLabel)) {
                    op.setLabel(labelMapOldNew.get(currentLabel));
                    func.addLabel(labelMapOldNew.get(currentLabel), op);
                }
                else {
                    Label newLabel = mainProgram.newUniqueLabel();
                    labelMapOldNew.put(op.getLabel(), newLabel);
                    op.setLabel(newLabel);
                    func.addLabel(newLabel, op);
                }
            }
            // if there is an exit label in the function, we need to remap it to a new unique label and assign it to the last op
            if(currentLabel != null && currentLabel.equals(FixedLabel.EXIT)) {
                containsExit = true;
                ExitLabel = mainProgram.newUniqueLabel();
                op.setLabel(ExitLabel);
            }
            if(op instanceof LabelJumper)
            {
                Label targetLabel = ((LabelJumper) op).getJumpLabel(); //useless casting but java is stupid
                Label newLabel;
                if(labelMapOldNew.containsKey(targetLabel))
                    newLabel = labelMapOldNew.get(targetLabel);
                else if (targetLabel.equals(FixedLabel.EXIT)) {
                    if (!op.equals(ops.getLast())) {
                        containsExit = true;
                        if (ExitLabel == null)
                            ExitLabel = mainProgram.newUniqueLabel();
                        newLabel = ExitLabel;
                    }
                    else {
                        newLabel = targetLabel;
                    }
                }
                else {
                    newLabel = mainProgram.newUniqueLabel();
                    labelMapOldNew.put(targetLabel, newLabel);
                }

                ((LabelJumper) op).setJumpLabel(newLabel); //again useless casting
            }
        }
        if(containsExit) {
            ops.getLast().setLabel(ExitLabel); //the last op is always constant assignment to result from y
            func.addLabel(ExitLabel, ops.getLast());
        }

         */
    }

    @Override
    public abstract List<AbstractOpBasic> expand(int ignoredExtensionLevel, FunctionExecutor ignoredProgram, VariableImpl Papa);

    @Override
    public abstract AbstractOpBasic myClone();

    @Override
    public int getCycles() {
        return super.getCycles() + funcCycles;
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<AbstractArgument> getArgs() {
        return functionArguments;
    }

    @Override
    public abstract String getRepresentation();

    protected List<AbstractOpBasic> expandFunction(FunctionExecutor func, VariableImpl resultVar, AbstractOpBasic parent)
    {
        List<AbstractOpBasic> ops = new ArrayList<>();
        Map<VariableImpl,VariableImpl> vars = new HashMap<>();
        AbstractOpBasic initOp = new OpNeutral(getVariable(), getLabel(), parent); //anchor
        ops.add(initOp);
        VariableImpl workVar;

        FunctionExecutor functionClone = function.myClone();

        if(getLabel() != null && !getLabel().equals(FixedLabel.EMPTY))
            func.addLabel(getLabel(), initOp);

        VariableImpl funcVar;
        // TODO : fix MainController
        /*
        MainController controller = MainApp.getMainController();
        SprogramImpl mainProgram = (SprogramImpl) controller.getMainProgram();


        if (functionArguments != null) {
             int idx = 0;

            for (AbstractArgument arg : functionArguments) {
                funcVar = functionClone.getNextVar(idx++);
                if (arg.getType().equals(AbstractArgument.ArgumentTypes.VARIABLE)) {
                    VariableArgument variableArgument = (VariableArgument) arg;
                    //   VariableImpl funcVar = variableArgument.getVariable();
                    if (funcVar.getType().equals(VariableType.RESULT))
                        workVar = resultVar;
                    else
                        workVar = mainProgram.newWorkVar();
                    if (funcVar.getType().equals(VariableType.INPUT)) {
                        AbstractOpBasic assigment = new OpAssignment(workVar, funcVar, parent);
                        ops.add(assigment);
                    }
                } else {
                    FunctionArgument funcArgument = (FunctionArgument) arg;
                    workVar = mainProgram.newWorkVar();
                    AbstractOpBasic opQuote = new OPQuote(workVar, funcArgument.getFunctionName(), funcArgument.getStrArguments(), mainProgram.getFunction(funcArgument.getFunctionName()), parent);
                    ops.add(opQuote);
                }
                vars.put(funcVar, workVar); // change key to val in function
            }
        } else {
            vars.put(VariableImpl.RESULT, resultVar);
        }

        for (VariableImpl var: functionClone.getAllVars()) {
            if (!vars.containsKey(var)) {
                if (var.getType().equals(VariableType.RESULT))
                    workVar = resultVar;
                else
                    workVar = mainProgram.newWorkVar();

                vars.put(var, workVar);
            }
        }

        functionClone.changeInputVar(vars);

        functionClone.getOps().forEach( op ->{
            op.setParent(parent);
            ops.add(op);
        });
        function = functionClone;
        historyFunctions.add(functionClone);

         */
        return ops;
    }

    public void collapse()
    {
        historyFunctions.remove(historyFunctions.getLast());
        function = historyFunctions.getLast();
    }
}
