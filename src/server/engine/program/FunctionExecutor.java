package server.engine.program;

import server.auth.UserProfile;
import server.engine.execution.ExecutionContext;
import server.engine.impl.api.skeleton.AbstractOpBasic;
import server.engine.label.Label;
import server.engine.variable.VariableImpl;
import shared.ExecutionStep;

import java.util.*;

public interface FunctionExecutor
{
    public String getName();
    FunctionExecutor getFunction(String functionName);
    void addLabelSet(LinkedHashSet<Label> labels);
    public List<Label> getLabelSet();
    void addOp(AbstractOpBasic instruction);
    List<AbstractOpBasic> getOps();
    int getNumInstuctions();
    int getCycles();
    int getCost();
    void increaseCycleCounter(int cycles);
    void reset();
    Label newUniqueLabel();
    VariableImpl getNextVar(int j);
    void createFirstSnap(List<Long> input);
    int getAmountOfVars();
    Long getVariableValue(VariableImpl var);
    void addSnap(ArrayList<VariableImpl> vars, ArrayList<Long> vals);
    AbstractOpBasic getOpByLabel(Label label);
    AbstractOpBasic getNextOp();
    void ChangeOpIndex(AbstractOpBasic currentOp);
    int getOpsIndex();
    void setInputVars(List<VariableImpl> vars);
    void setInputVars(Set<VariableImpl> vars) ;
    int getInputVarSize();
    void setAllVars(Set<VariableImpl> inputVars);
    Set<VariableImpl> getAllVars();
    void addLabel(Label label, AbstractOpBasic op);
    FunctionExecutor myClone();
    int getProgramDegree();
    void expandProgram(int degree);
    void expandSingle(AbstractOpBasic opToExpand, int degree);
    VariableImpl newWorkVar();
    void setContext(ExecutionContext context);
    Map<VariableImpl, Long> getCurrSnap();
    void collapse();
    void collapseSingle(AbstractOpBasic op, int i);
    void opListIndexReset();
    List<VariableImpl> getInputVar();
    void init();
    void restoreOriginalVars();
    void resetSnap();
    int run(List<Long> inputs, List <FunctionExecutor> functions, List<ExecutionStep> executionDetails, UserProfile owner, boolean b);
    void changeInputVar(Map<VariableImpl, VariableImpl> vars);
    void setParentProgram(SprogramImpl chosenMainProgram);
    SprogramImpl getParentProgram();
    String getUserString();
    void setFunctions( Set<String> funcNameList);
    Set<String> getFuncNameList();
}
