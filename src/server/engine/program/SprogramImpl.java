package server.engine.program;

import server.engine.execution.ProgramCollection;
import server.engine.impl.api.skeleton.AbstractOpBasic;
import server.engine.label.Label;

import java.util.*;

public class SprogramImpl extends FunctionExecutorImpl {

    List <FunctionExecutor> functions;
    Set<String> funcNameList;

    public SprogramImpl(String name) {
        super(name);
        functions = null;
    }

    public SprogramImpl(FunctionExecutorImpl other) {
        super(other.getName());

        if (other.opList != null) {
            for (AbstractOpBasic op : other.opList) {
                addOp(op.myClone());
            }
        }
        this.cost = other.cost;
        this.averageCost = other.averageCost;
        this.maxDegree = other.maxDegree;
        this.inputVars = other.inputVars != null ? new ArrayList<>(other.inputVars) : null;
        setContext(other.context);
        updateLabels();
        this.variables = other.variables != null ? new HashSet<>(other.variables) : null;
        this.origVariables = other.origVariables != null ? new HashSet<>(other.origVariables) : null;
        this.labelsHashSet = other.labelsHashSet != null ? new LinkedHashSet<>(other.labelsHashSet) : null;
        this.setUserString(other.getUserString());
        functions = null;
    }

    public void addFunction(FunctionExecutor func) {
        if (functions == null)
            functions = new ArrayList<>();
        functions.add(func);
    };

    public void setFunctions( Set<String> funcNameList) {
        this.funcNameList = new HashSet<>(funcNameList);
    }

    public Set<String> getFuncNameList() {
        return funcNameList;
    }

    public List<FunctionExecutor> getFunctions() {
        return functions;
    }

    public void updateFunctions() {
        if (functions == null)
            functions = new ArrayList<>();
        functions.clear();
        if (funcNameList != null) {
            funcNameList.forEach(funcName -> {
                FunctionExecutor func = ProgramCollection.getFunction(funcName);
                if (func != null) {
                    FunctionExecutor funcClone = func.myClone();
                    funcClone.setParentProgram(this);
                    functions.add(funcClone);
                }
            });
        }
        updateOps();

    }
    private void updateOps()
    {
        if (functions == null)
            return;
        updateFunctionOps();

        functions.forEach(func->{
            ((FunctionExecutorImpl)func).updateFunctionOps();
        });
    }

    public void calculateAverageCost()
    {
        double totalCost = 0.0;
        int NUM_TESTS = 3;
        int nInputs = inputVars.size();
        List<Long> userVars = new ArrayList<>();
        for (int i = 0; i < NUM_TESTS; i++) {
            userVars.clear();
            for (int j = 0; j < nInputs; j++) {
                long val = (long) (Math.random() * 100);
                userVars.add(val);
            }
            reset();
            run(userVars, functions, null, null, false);
            totalCost += getCost();
            totalCost += getCycles();
        }

        this.averageCost = (int) (totalCost / NUM_TESTS);
    }

    public void reset() {
        this.cycles = 0;
        this.opListIndex = 0;
        context.reset();
        if (functions != null) {
            functions.forEach(FunctionExecutor::reset);
        }
    }

    @Override
    public void opListIndexReset()
    {
        this.opListIndex = 0;
        if (functions != null) {
            functions.forEach(FunctionExecutor::opListIndexReset);
        }
    }

    @Override
    public FunctionExecutor getFunction(String functionName) {
        if (functions == null)
            return null;
        else for (FunctionExecutor func : functions) {
            if (func.getName().equals(functionName))
                return func;
        }
        return null;
    }

    // deep clone
    @Override
    public FunctionExecutor myClone() {
        SprogramImpl newProgram = new SprogramImpl(this.name);
        for (AbstractOpBasic op : this.opList) {
            newProgram.addOp( op.myClone()); // Assuming AbstractOpBasic is immutable or properly cloned
        }
        newProgram.setInputVars(new ArrayList<>(this.inputVars));
        newProgram.setAllVars(new HashSet<>(this.variables));
        newProgram.addLabelSet(new LinkedHashSet<>(this.labelsHashSet));
        newProgram.origVariables = new HashSet<>(this.origVariables);
        newProgram.cost = this.cost;
        newProgram.maxDegree = this.maxDegree;
        newProgram.averageCost = this.getAverageCost();
        newProgram.setFunctions(this.funcNameList);
        newProgram.averageCost = this.averageCost;
        newProgram.setContext(context);
        newProgram.updateLabels();

        if (functions != null) {
            functions.forEach(func->{
                FunctionExecutor newFunc = func.myClone();
                newProgram.addFunction(newFunc);
            });
        }
        newProgram.updateOps();

        return newProgram;
    }

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void restoreOriginalVars() {
        super.restoreOriginalVars();

        if (functions == null)
            return;
        functions.forEach(FunctionExecutor::restoreOriginalVars);
    }

    @Override
    public void resetSnap() {
        super.resetSnap();

        if (functions == null)
            return;
        functions.forEach(FunctionExecutor::resetSnap);
    }
}
