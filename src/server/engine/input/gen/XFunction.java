package server.engine.input.gen;

import jakarta.xml.bind.annotation.*;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class XFunction {

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "user-string")
    private String userString;

    @XmlElementWrapper(name = "S-Instructions")
    @XmlElement(name = "S-Instruction")
    private List<XOp> instructions;

    public String getName() { return name; }
    public String getUserString() { return userString; }
    public List<XOp> getInstructions() { return instructions; }

}
