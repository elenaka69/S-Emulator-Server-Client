package server.engine.impl.api.skeleton;

public enum OpType {
    BASIC {
        @Override
        public String getType() {
            return "BASIC";
        }
    },
    SYNTHETIC{
        @Override
        public String getType() {
            return "SYNTHETIC";
        }
    };
    public abstract String getType();
}
