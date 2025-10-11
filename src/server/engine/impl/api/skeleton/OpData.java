package server.engine.impl.api.skeleton;


public enum OpData {

    INCREASE("INCREASE" , 1,0, 5, OpType.BASIC),
    DECREASE("DECREASE",1,0, 5, OpType.BASIC),
    JUMP_NOT_ZERO("JUMP_NOT_ZERO",2,0, 5,OpType.BASIC),
    NEUTRAL("NEUTRAL",0,0, 5, OpType.BASIC),
    ZERO_VARIABLE("ZERO_VARIABLE",1,1, 100, OpType.SYNTHETIC),
    GOTO_LABEL("GOTO_LABEL",1,1, 100, OpType.SYNTHETIC),
    CONSTANT_ASSIGNMENT("CONSTANT_ASSIGNMENT",2,2, 100, OpType.SYNTHETIC),
    ASSIGNMENT("ASSIGNMENT",4,2, 500, OpType.SYNTHETIC),
    JUMP_ZERO("JUMP_ZERO",2,2, 500, OpType.SYNTHETIC),
    JUMP_EQUAL_CONSTANT("JUMP_EQUAL_CONSTANT",2,3, 500, OpType.SYNTHETIC),
    JUMP_EQUAL_VARIABLE("JUMP_EQUAL_VARIABLE",2,2, 500, OpType.SYNTHETIC),
    QUOTE("QUOTE",5,0, 1000, OpType.SYNTHETIC), // degree value will be calculated later
    JUMP_EQUAL_FUNCTION("JUMP_EQUAL_FUNCTION",6,0, 1000, OpType.SYNTHETIC); // degree value will be calculated later


    private final String name;
    private final int cycles;
    private int degree;
    private final OpType type;
    private final int credit;

    OpData(String name, int cycles, int degree, int credit, OpType type) {
        this.name = name;
        this.cycles = cycles;
        this.type = type;
        this.degree = degree;
        this.credit = credit;
    }

    public String getName() {
        return name;
    }
    public int getCycles() {
        return cycles;
    }
    public int getDegree() {
        return degree;
    }
    public OpType getType() {
        return type;
    }
    public int getCredit() { return credit; }

    public void setDegree(int degree) {
        this.degree = degree;
    }
}
