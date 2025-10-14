package server.engine.impl.api.skeleton;


public enum OpData {

    INCREASE("INCREASE" , 1,0, 5, 1, OpType.BASIC),
    DECREASE("DECREASE",1,0, 5,1,  OpType.BASIC),
    JUMP_NOT_ZERO("JUMP_NOT_ZERO",2,0, 5, 1, OpType.BASIC),
    NEUTRAL("NEUTRAL",0,0, 5, 1, OpType.BASIC),
    ZERO_VARIABLE("ZERO_VARIABLE",1,1, 100, 2, OpType.SYNTHETIC),
    GOTO_LABEL("GOTO_LABEL",1,1, 100, 2, OpType.SYNTHETIC),
    CONSTANT_ASSIGNMENT("CONSTANT_ASSIGNMENT",2,2, 100, 2, OpType.SYNTHETIC),
    ASSIGNMENT("ASSIGNMENT",4,2, 500, 3, OpType.SYNTHETIC),
    JUMP_ZERO("JUMP_ZERO",2,2, 500, 3, OpType.SYNTHETIC),
    JUMP_EQUAL_CONSTANT("JUMP_EQUAL_CONSTANT",2,3, 500, 3, OpType.SYNTHETIC),
    JUMP_EQUAL_VARIABLE("JUMP_EQUAL_VARIABLE",2,2, 500, 3, OpType.SYNTHETIC),
    QUOTE("QUOTE",5,0, 1000, 4, OpType.SYNTHETIC), // degree value will be calculated later
    JUMP_EQUAL_FUNCTION("JUMP_EQUAL_FUNCTION",6,0, 1000, 4, OpType.SYNTHETIC); // degree value will be calculated later


    private final String name;
    private final int cycles;
    private int degree;
    private final OpType type;
    private final int credit;
    private final int arch;

    OpData(String name, int cycles, int degree, int credit, int arch, OpType type) {
        this.name = name;
        this.cycles = cycles;
        this.type = type;
        this.degree = degree;
        this.credit = credit;
        this.arch = arch;
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
    public int getArch() { return arch; }

    public void setDegree(int degree) {
        this.degree = degree;
    }
}
