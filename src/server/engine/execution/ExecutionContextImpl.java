package server.engine.execution;

import server.engine.impl.api.skeleton.AbstractOpBasic;
import server.engine.label.FixedLabel;
import server.engine.label.*;
import server.engine.program.FunctionExecutor;
import server.engine.variable.VariableImpl;
import server.engine.variable.VariableType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExecutionContextImpl implements ExecutionContext, ExpandContext {

    private ArrayList<Map<VariableImpl, Long>> snapshots; // to turn off the comment
    private Map<VariableImpl, Long> currSnap;
    private Map<Label, AbstractOpBasic> labelMap;

    public int getLabelindex() {
        return labelindex;
    }

    private int labelindex = 1;

    public int getWorkVarIndex() {
        return workVarIndex;
    }

    private int workVarIndex = 1;



    public void setLabelMap(FunctionExecutor program) {
        labelMap = new HashMap<>();
        for (var op : program.getOps()) {
            if (op.getLabel() != FixedLabel.EMPTY) // we don't want to add empty labels to the map
                labelMap.put(op.getLabel(), op);
        }
    }
    //create a deep copy constructor
    public ExecutionContextImpl(ExecutionContext context) {
        //create a deep copy constructor
        snapshots = new ArrayList<>(context.getSnapshots());
        currSnap = new HashMap<>(context.getCurrSnap());
        labelMap = new HashMap<>(context.getLabelMap());
        labelindex = getLabelindex();
        workVarIndex = getWorkVarIndex();

    }
    public Map<VariableImpl, Long> getCurrSnap()
    {
        return Map.copyOf(currSnap);
    }


    @Override
    public Map<Label, AbstractOpBasic> getLabelMap() {
        return labelMap;
    }

    public ExecutionContextImpl() {
        snapshots = new ArrayList<>(); // move to another function that will handle inputs from the user
        labelMap = new HashMap<>();
        currSnap = new HashMap<>();
    }

    @Override
    public List<Map<VariableImpl, Long>> getSnapshots() {
        return snapshots;
    }

    @Override
    public void reset() {
        snapshots.clear();
        currSnap.clear();
    }

    public void createSnap(FunctionExecutor program, List<Long> input) {

        Map<VariableImpl, Long> snap = new HashMap<>();
        VariableImpl tmp;

        for (int i = 0; i < program.getAmountOfVars(); i++) { //fills all the input var with the input and the rest with 0
            tmp = program.getNextVar(i);
            if (i < input.size())
                snap.put(tmp, input.get(i));
            else
                snap.put( tmp ,0L);
        }

        for(VariableImpl v : program.getAllVars()) // make sure all vars are in the snap and if not add them with value 0
        {
            snap.computeIfAbsent(v, k -> 0L);
        }
       snap.put(VariableImpl.RESULT, 0L); //add the result var
       var first = Map.copyOf(snap);  // making an immutable copy
       snapshots.add(first);
       currSnap = snap;
    }

    public Long getVariableValue(VariableImpl v) {return currSnap.get(v);}

    @Override
    public void addSnap(ArrayList<VariableImpl> vars, ArrayList<Long> vals) {
        if (vars.size() != vals.size()) {
            throw new IllegalArgumentException("vars and vals must have the same length");
        }

       for (int i  = 0; i < vals.size(); i++) {
           currSnap.put(vars.get(i),vals.get(i)); // the current snapshot
        }
        var first = Map.copyOf(currSnap);  // making an immutable copy
        snapshots.add(first);
    }


    @Override
    public Label newUniqueLabel() {
        while (labelMap.containsKey(new LabelImpl(labelindex++))) {//ignore and just raise the index
             } //empty beacuse the ++ is needed
        return new LabelImpl(labelindex-1);
    }

    @Override
    public VariableImpl newWorkVar() {
        VariableImpl tmp;
        while (currSnap.containsKey(new VariableImpl(VariableType.WORK,workVarIndex)))
        {
            workVarIndex++;
        };
        tmp =  new VariableImpl(VariableType.WORK,workVarIndex);
        currSnap.put(tmp,0L);
        return tmp;
    }

    @Override
    public void addOpWithNewLabel(AbstractOpBasic op ) {
        labelMap.put(newUniqueLabel(),op);
    }
}
