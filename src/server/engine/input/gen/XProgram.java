package server.engine.input.gen;
import jakarta.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "S-Program")
@XmlAccessorType(XmlAccessType.FIELD)
public class XProgram {
    @XmlAttribute(name = "name")
    private String name;

    @XmlElementWrapper(name = "S-Instructions")
    @XmlElement(name = "S-Instruction")
    private List<XOp> xOpList;  // contains the list of SInstruction

    @XmlElementWrapper(name = "S-Functions")
    @XmlElement(name = "S-Function")
    private List<XFunction> functions;

    public String getName() { return name; }
    public List<XOp> getInstructions() { return xOpList; }
    public List<XFunction> getFunctions() { return functions; }

}


