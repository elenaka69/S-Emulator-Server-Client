package server.engine.label;

public enum FixedLabel implements Label {

    EXIT{
        @Override
        public  String getLabelRepresentation() {return "EXIT";}

        @Override
        public Label myClone() {
            return this;
        }
    },
    EMPTY{
        @Override
        public String getLabelRepresentation() {return "";}
        public   Label myClone() {
            return this;
        }
    };

    @Override
    public abstract String getLabelRepresentation();

}
