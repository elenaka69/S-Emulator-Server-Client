package server.engine.program;


import server.engine.execution.ExecutionContext;
import server.engine.execution.ExecutionContextImpl;
import server.engine.impl.api.skeleton.AbstractOpBasic;
import server.engine.impl.api.skeleton.VariableUser;
import server.engine.impl.api.skeleton.functionArgs.AbstractArgument;
import server.engine.impl.api.skeleton.functionArgs.FunctionArgument;
import server.engine.impl.api.skeleton.functionArgs.VariableArgument;
import server.engine.impl.api.synthetic.OpFunctionBase;
import server.engine.label.*;
import server.engine.variable.VariableImpl;

import java.util.*;
import javafx.util.Pair;

public class FunctionExecutorImpl implements FunctionExecutor {

    protected final String name;
    protected  List<AbstractOpBasic> opList;
    protected int cycles;
    protected int opListIndex;
    protected List<VariableImpl> inputVars;
    protected ExecutionContextImpl context;
    protected Set<VariableImpl> variables;
    protected Set<VariableImpl> origVariables;
    protected LinkedHashSet <Label> labelsHashSet;
    private String userString;
    protected int cost = 0;
    protected int maxDegree;
    private SprogramImpl parentProgram = null;

    public void setParentProgram(SprogramImpl parentProgram) {
        this.parentProgram = parentProgram;
    }

    public SprogramImpl getParentProgram() {
        return parentProgram;
    }

    public FunctionExecutorImpl(String name) {
        this.name = name;
        opList = new ArrayList<>();
        opListIndex = 0;
        context = new ExecutionContextImpl();
        cycles = 0;
        variables = new HashSet<>();
        labelsHashSet = new LinkedHashSet<>();
        inputVars = new ArrayList<>();
    }

    public int getCost() {
        return cost;
    }

    public String getUserString() { return userString; }

    public void setUserString(String userString) { this.userString = userString; }

    @Override
    public String getName() { return name; }
    @Override
    public void setAllVars(Set<VariableImpl> inputVars)
    {
        variables.addAll( inputVars);
    }
    @Override
    public Set<VariableImpl> getAllVars()
    {
        return variables;
    }
    @Override
    public Long getVariableValue(VariableImpl variable) { return context.getVariableValue(variable); }
    @Override
    public VariableImpl getNextVar(int i) {
        return inputVars.get(i);
    }
    @Override
    public int getAmountOfVars() {
        return inputVars.size();
    }
    @Override
    public void addLabel(Label label, AbstractOpBasic op)
    {
        context.getLabelMap().put(label,op);
    }
    @Override
    public AbstractOpBasic getOpByLabel(Label label) { return context.getLabelMap().get(label); }
    @Override
    public void addLabelSet(LinkedHashSet<Label> labels) {this.labelsHashSet = labels;}
    @Override
    public List<Label> getLabelSet()    { return new ArrayList<>(context.getLabelMap().keySet());  }

    @Override
    public void createFirstSnap(List<Long> input) {
        context.createSnap(this, input);
    }
    @Override
    public Map<VariableImpl, Long> getCurrSnap()
    {
        return context.getCurrSnap();
    }
    @Override
    public void addSnap(ArrayList<VariableImpl> vars, ArrayList<Long> vals) {context.addSnap(vars, vals);}

    @Override
    public void setInputVars(List<VariableImpl> vars) {
        this.inputVars = vars;
    }
    @Override
    public void setInputVars(Set<VariableImpl> vars) {
        this.inputVars = new ArrayList<>(vars);
    }
    @Override
    public int getInputVarSize()
    {
        return inputVars.size();
    }

    @Override
    public List<VariableImpl> getInputVar()
    {
        return inputVars;
    }

    @Override
    public void setContext(ExecutionContext context) { this.context = new ExecutionContextImpl(context); }
    @Override
    public FunctionExecutor getFunction(String functionName) {return null;};

    @Override
    public int getCycles() {
        return cycles;
    }
    public void increaseCycleCounter(int cycles) { this.cycles += cycles; }

    @Override
    public void init() {
        origVariables = new HashSet<>(variables);
        calculateCost();
        calculateQuoteDegree();
        calculateMaxDegree();
    }

    @Override
    public void restoreOriginalVars() {
        variables = new HashSet<>(origVariables);
    }

    @Override
    public void resetSnap() {
        context.reset();
        List<Long> zeros = new ArrayList<>(Collections.nCopies(getAmountOfVars(), 0L));
        createFirstSnap(zeros);
    }

    @Override
    public List<AbstractOpBasic> getOps() { return opList; }
    @Override
    public int getOpsIndex()
    {
        return opListIndex;
    }
    @Override
    public void addOp(AbstractOpBasic op) { opList.add(op); }
    public AbstractOpBasic getNextOp() {
        if ( opListIndex >= opList.size()) {
            opListIndex = 0;
            return null;
        }
        return opList.get(opListIndex++);
    }
    @Override
    public void ChangeOpIndex(AbstractOpBasic currentOp) {
        if (currentOp==null)
            throw(new IllegalArgumentException("the op is null"));
        else if (opList.stream().anyMatch(op -> op.getUniqId().equals(currentOp.getUniqId())))
            opListIndex = opList.indexOf(opList.stream()
                    .filter(op -> op.getUniqId().equals(currentOp.getUniqId()))
                    .findFirst().get());
        else
            throw(new IllegalArgumentException("the op is not in the program"));

        opListIndex++;
    }

    @Override
    public void opListIndexReset()
    {
        this.opListIndex = 0;
    }

    public void reset() {
        this.cycles = 0;
        this.opListIndex = 0;
        context.reset();
    }

    @Override
    public Label newUniqueLabel() { return context.newUniqueLabel(); }

    @Override
    public int getProgramDegree() {
        return maxDegree;
    }

    protected void calculateMaxDegree() {
        int maxDegree = 0;
        int degree;
        for (AbstractOpBasic op : opList) {
            degree = op.getDegree();
            if (degree > maxDegree) {
                maxDegree = degree;
            }
        }
        this.maxDegree = maxDegree;
    }

    public void calculateCost()
    {
        int totalPrice = 0;
        for (AbstractOpBasic op : opList) {
            totalPrice += op.getCredit();
        }
        this.cost = totalPrice;
    }

    public void calculateQuoteDegree() {
        opList.forEach(op -> {
            if (op instanceof OpFunctionBase opFunctionBase) {
                opFunctionBase.setDegree();
            }
        });
    }

    public void changeInputVar(Map <VariableImpl,VariableImpl> vars)
    {
        // change input vars to new work vars from expansion
        for (AbstractOpBasic op : opList) {
            op.setVariable(vars.get(op.getVariable()));
            if (op instanceof VariableUser) { //change  the other vars in the op if there are any
                VariableImpl secondaryVarOld =   ((VariableUser) op).getSecondaryVariable();
                VariableImpl secondaryVarNew = vars.get(secondaryVarOld);
                ((VariableUser) op).setSecondaryVariable(secondaryVarNew);
        }
        }
    }

    protected  void updateFunctionOps() {
        for (AbstractOpBasic op : opList) {
            if (op instanceof OpFunctionBase) {
                String funName = ((OpFunctionBase) op).getFunctionName();
                FunctionExecutor func;
                if (parentProgram != null)
                    func = parentProgram.getFunction(funName);
                else
                    func = getFunction(funName);
                if (func != null) {
                    ((OpFunctionBase) op).setFunction(func);
                }
            }
        }
    }

    @Override
    public FunctionExecutor myClone() {
        FunctionExecutorImpl newFunc = new FunctionExecutorImpl(this.name);
        for (AbstractOpBasic op : this.opList) {
            newFunc.addOp( op.myClone()); // Assuming AbstractOpBasic is immutable or properly cloned
        }
        newFunc.setInputVars(new ArrayList<>(this.inputVars));
        newFunc.setAllVars(new HashSet<>(this.variables));
        newFunc.setContext(context);
        newFunc.addLabelSet(new LinkedHashSet<>(this.labelsHashSet));
        newFunc.setUserString(this.userString);
        newFunc.origVariables = new HashSet<>(this.origVariables);
        newFunc.cost = this.cost;
        newFunc.maxDegree = this.maxDegree;
        newFunc.parentProgram = this.parentProgram;
        return newFunc;
    }

    public VariableImpl newWorkVar()
    {
        VariableImpl tmp = context.newWorkVar();
        variables.add(tmp);
        return tmp;
    }

    public void expandProgram(int degree)
    {
        List<AbstractOpBasic> expandedList = new  ArrayList<>();
        for (AbstractOpBasic op: opList) {
            expandedList.addAll(op.expand(degree,this));
        }
        opList = expandedList;
        updateVariables();
    }

    public void expandSingle(AbstractOpBasic opToExpand, int degree)
    {
        List<AbstractOpBasic> expandedList = new  ArrayList<>();
        for (AbstractOpBasic op: opList) {
            if(op.getUniqId().equals(opToExpand.getUniqId())) {
                expandedList.addAll(op.expand(degree,this));
            } else
                expandedList.add(op);
        }
        opList = expandedList;
        updateVariables();
    }

    @Override
    public void collapse() {
        int maxDepth = 0;

        for (AbstractOpBasic op: opList) {
            int opDepth = op.calculateDepth();
            maxDepth = Math.max(opDepth, maxDepth);
        }

        List<AbstractOpBasic> expandedList = new  ArrayList<>();
        Set<AbstractOpBasic> seenParents = new HashSet<>();
        for (AbstractOpBasic op: opList) {
            AbstractOpBasic parent = op.getParent();
            if (parent == null || op.getDepth() != maxDepth)
            {
                expandedList.add(op);
            } else {
                if (!seenParents.contains(parent)) {
                    if (parent instanceof OpFunctionBase) {
                        ((OpFunctionBase) parent).collapse();
                    }
                    expandedList.add(parent);
                    seenParents.add(parent);
                }
            }
        }
        opList = expandedList;
        updateLabelsAfterCollapse();
        updateVariables();
    }

    @Override
    public void collapseSingle(AbstractOpBasic opToCollapse, int i) {
        AbstractOpBasic parent = opToCollapse.getParent();
        if (parent == null)
            return;
        if (parent instanceof OpFunctionBase) {
            ((OpFunctionBase) parent).collapse();
        }
        boolean isParentAded = false;
        List<AbstractOpBasic> expandedList = new ArrayList<>();

        for (AbstractOpBasic op : opList) {
            if (parent != op.getParent()) {
                expandedList.add(op);
            } else {
                if (!isParentAded) {
                    expandedList.add(parent);
                    isParentAded = true;
                }
            }
        }

        opList = expandedList;
        updateLabelsAfterCollapse();
        updateVariables();
    }

    private void updateVariables()
    {
        variables.clear();
        for (AbstractOpBasic op : opList) {
            variables.add(op.getVariable());
            if (op instanceof VariableUser) { // add the other var in the op if there are any
                variables.add(((VariableUser) op).getSecondaryVariable());
            }
        }
    }

    private void updateLabelsAfterCollapse() {
        // Rebuild the label map in the context
        context.getLabelMap().clear();
        for (AbstractOpBasic op : opList) {
            if (op.getLabel() != FixedLabel.EMPTY && op.getLabel() != FixedLabel.EXIT) { // we don't want to add empty labels to the map
                context.getLabelMap().put(op.getLabel(), op);
            }
        }
    }

    @Override
    public List<Pair<Integer, TreeMap<VariableImpl, Long>>> run(List<Long> inputs, List <FunctionExecutor> functions) {
        List<Pair<Integer, TreeMap<VariableImpl, Long>>> listOfRunSteps = new ArrayList<>();
        TreeMap<VariableImpl, Long> runMap = new TreeMap<>(Comparator.comparing(VariableImpl::getRepresentation));

        reset();
        resetSnap();
        createFirstSnap(inputs);  // enter the vals from the user to the input vars
        AbstractOpBasic current = getNextOp();

        // put init state
        runMap.putAll(getCurrSnap());
        listOfRunSteps.add(new Pair<>(getOpsIndex()-1, runMap));

        while (current != null) {
            Label next;
            if (current instanceof OpFunctionBase)
                next = ((OpFunctionBase)current).execute(this, functions);
            else
                next = current.execute(this);

            if (next.equals(FixedLabel.EXIT)) {
                break;
            } else if (next.equals( FixedLabel.EMPTY )) {
                current = getNextOp();
            } else {
                AbstractOpBasic target = getOpByLabel(next);
                if (target == null) {
                    throw new IllegalStateException(
                            "Jump to undefined label: " + next.getLabelRepresentation());
                }
                ChangeOpIndex(target);
                current = target;
            }
            runMap = new TreeMap<>(Comparator.comparing(VariableImpl::getRepresentation));
            runMap.putAll(getCurrSnap());
            listOfRunSteps.add(new Pair<>(getOpsIndex()-1, runMap));
        }
        return listOfRunSteps;
    }

    public Long  run(FunctionExecutor program, List<AbstractArgument> functionArguments, List <FunctionExecutor> functions) throws IllegalArgumentException
    {
        List<Long> funcVars = new ArrayList<>();
        if ( functionArguments != null) {
            functionArguments.forEach(arg-> {
                if (arg.getType().equals(AbstractArgument.ArgumentTypes.VARIABLE)) {
                    funcVars.add(program.getVariableValue(((VariableArgument) arg).getVariable()));
                } else {
                    // ArgumentTypes.FUNCTION
                    String argFuncName = ((FunctionArgument) arg).getFunctionName();
                    FunctionExecutorImpl argFunc = null;
                    for (FunctionExecutor func : functions) {
                        if (argFuncName.equals(func.getName())) {
                            argFunc = (FunctionExecutorImpl)func;
                            break;
                        }
                    }
                    if (argFunc == null)
                        throw new IllegalArgumentException("function " + argFuncName + " not found");

                    List<AbstractArgument> argFuncArgs = ((FunctionArgument) arg).getArgument();
                    Long res =  argFunc.run(program, argFuncArgs, functions);
                    funcVars.add(res);
                }
            });
        }

        run(funcVars, functions);

        return getVariableValue(VariableImpl.RESULT);
    }
    public void addInputVar(VariableImpl var) { inputVars.add(var);}
    public void addVar(VariableImpl var) { variables.add(var); }
}
