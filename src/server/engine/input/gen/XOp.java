package server.engine.input.gen;
import jakarta.xml.bind.annotation.*;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)

public class XOp {
    @XmlAttribute(name = "name")
    private String name; // Increase / Decrease / JNZ

    @XmlAttribute(name = "type")
    private String type; // basic / synthetic

    @XmlElement(name = "S-Label")
    private String label; // optional l1, l2, ...

    @XmlElement(name = "S-Variable")
    private String variable; //x1,z3, y etc.

    @XmlElementWrapper(name = "S-Instruction-Arguments")
    @XmlElement(name = "S-Instruction-Argument")
    private List<XOpArguments> arguments; // optional like constants for synthetic ops

    public String getName() { return name; }
    public String getType() { return type; }
    public String getLabel() { return label; }
    public String getVariable() { return variable; }
    public List<XOpArguments> getArguments() { return arguments; }
}
