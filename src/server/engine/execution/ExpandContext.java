package server.engine.execution;

import server.engine.impl.api.skeleton.AbstractOpBasic;
import server.engine.label.Label;
import server.engine.variable.VariableImpl;

public interface ExpandContext {
    Label newUniqueLabel();

    /** מחזיר משתנה עבודה חדש בפורמט z<number> (ייחודי), מאופס ומוסף ל-currSnap ולסט המשתנים. */
    VariableImpl newWorkVar();

    void addOpWithNewLabel(AbstractOpBasic op);


}

