package server.engine.label;

import java.util.Objects;

public class LabelImpl implements Label {
    private final String label;

    public LabelImpl(int number) {
        this.label = "L" + number;
    }
    public LabelImpl(String label) {
        this.label = label;
    }
    public String getLabelName() {
        return this.label;
    }

    @Override
    public String getLabelRepresentation() {return this.label;}

    @Override
    //implement equals and hashcode based on label string
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LabelImpl label1 = (LabelImpl) obj;
        return label.equals(label1.getLabelRepresentation());
    }
    @Override
    //implement hashcode based on label string
    public int hashCode() {
        return Objects.hash(getLabelRepresentation());
    }
    public Label myClone() {
        return new LabelImpl(this.label);
    }


}

