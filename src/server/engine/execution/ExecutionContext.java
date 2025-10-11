package server.engine.execution;

import server.engine.impl.api.skeleton.AbstractOpBasic;
import server.engine.label.Label;
import server.engine.variable.VariableImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface ExecutionContext {

    Long getVariableValue(VariableImpl v);
    Map<Label, AbstractOpBasic> getLabelMap();
    void addSnap(ArrayList<VariableImpl> vars, ArrayList<Long> vals);
    Map<VariableImpl, Long> getCurrSnap();
    List<Map<VariableImpl, Long>> getSnapshots();
    void reset();
}