package server.engine.variable;
import java.util.Objects;

public class VariableImpl
{
    private final VariableType type;
    private final int number;
    public static VariableImpl RESULT = new VariableImpl(VariableType.RESULT, 0);

    public VariableImpl(VariableType type, int number) {
        this.type = type;
        this.number = number;
    }

    public VariableImpl(char type, int number) {
        this.type = typeFromChar(type);
        this.number = number;
    }

    public VariableImpl(String strVar) {
        this(strVar.charAt(0), Integer.parseInt(strVar.substring(1)));
    }

    private VariableType typeFromChar(char type) {
        return switch (type) {
            case 'x' -> VariableType.INPUT;
            case 'y' -> VariableType.RESULT;
            case 'z' -> VariableType.WORK;
            default -> throw new IllegalArgumentException("Invalid variable type character: " + type);
        };
    }

    public VariableType getType() {
        return type;
    }

    public String getRepresentation() {
        return type.getVariableRepresentation(number);
    }

    @Override
    public int hashCode() {//implement hashcode based on variable representation
        return Objects.hash(getRepresentation());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(getRepresentation(), ((VariableImpl) o).getRepresentation());
    }

  public VariableImpl myClone() {
        return  new VariableImpl(type, number);
    }

}
