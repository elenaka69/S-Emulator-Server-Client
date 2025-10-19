package server.engine.input.XmlTranslator;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import server.engine.execution.EngineManager;
import server.engine.execution.ProgramCollection;
import server.engine.impl.api.basic.*;
import server.engine.impl.api.skeleton.AbstractOpBasic;
import server.engine.impl.api.synthetic.*;
import server.engine.input.gen.XFunction;
import server.engine.input.gen.XOp;
import server.engine.input.gen.XProgram;
import server.engine.label.*;
import server.engine.program.FunctionExecutor;
import server.engine.execution.ERROR_CODES;
import server.engine.program.FunctionExecutorImpl;
import server.engine.program.SprogramImpl;
import server.engine.input.gen.*;
import server.engine.variable.VariableImpl;
import server.engine.variable.VariableType;


import java.util.*;

class instructionCollect {
    private LinkedHashSet<Label> definedLabels;
    private List<XOp> sInstructions;

    public instructionCollect(List<XOp> instructions)
    {
        this.sInstructions = instructions;
    };

    public void extractLabels()
    {
        // Collect all labels defined in the program
        definedLabels = new LinkedHashSet<>();
        for (XOp inst : sInstructions) {
            if (inst.getLabel() != null) {
                if (inst.getLabel().equals(FixedLabel.EXIT.getLabelRepresentation()))
                    definedLabels.add(FixedLabel.EXIT);
                else
                    definedLabels.add(new LabelImpl(inst.getLabel()));
            }
        }
    }
    public LinkedHashSet<Label> getDefinedLabels() { return definedLabels; }
    public List<XOp> getInstructions() { return sInstructions; }
}

public class Factory
{
    XProgram xProgram;

    FunctionExecutor program;
    private List<XFunction> xFunctions;
    private final Set<String> functions = new HashSet<>();
    private final List<String> functionNamesOfProgram = new ArrayList<>();

    public int loadProgramFromXml(String username, String fileName, java.io.InputStream xmlStream) throws IllegalArgumentException {
        instructionCollect  collection;
        int res;

        program = null;

        functions.clear();
        functionNamesOfProgram.clear();

        try {
            xProgram = loadXml(xmlStream);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw e;
        }
        try {
            collection = initFactory();
            validateLabels(collection.getInstructions(), collection.getDefinedLabels());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw e;
        }
        try {
            res = extractFunctions(username, fileName);
            if (res != ERROR_CODES.ERROR_OK) {
                removeFunctions();
                return res;
            }
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
            removeFunctions();
            throw e;
        }

        try {
            res = buildProgram(program, collection.getInstructions(), collection.getDefinedLabels());
            if (res != ERROR_CODES.ERROR_OK) {
                removeFunctions();
                return res;
            }
        } catch (IllegalArgumentException e) {
            removeFunctions();
            throw e;
        }
        if (collection.getInstructions().size() == 1 && collection.getInstructions().get(0).getName().equals("NEUTRAL"))
            return ERROR_CODES.ERROR_PROGRAM_WO_INSTRUCTIONS;

        ((SprogramImpl)program).setFunctions(functions);
        ((SprogramImpl) program).calculateCost();
        ProgramCollection.registerProgram(username, fileName, program);
        return ERROR_CODES.ERROR_OK;
    }

    private void removeFunctions()
    {
        functionNamesOfProgram.forEach(ProgramCollection::removeFunction);
        functionNamesOfProgram.clear();
        functions.clear();
    }

    //opens the xml file and loads it into an XProgram object
    private XProgram loadXml(java.io.InputStream xmlStream) throws IllegalArgumentException {
        if (xmlStream == null) {
            throw new IllegalArgumentException("XML input stream is null" );
        }
        // 2. Unmarshal XML into FunctionExecuter object using JAXB
        try {
            JAXBContext context = JAXBContext.newInstance(XProgram.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            xProgram =  (XProgram) unmarshaller.unmarshal(xmlStream);
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to parse XML: " + e.getMessage(), e);
        }
        return xProgram;
    }

    private instructionCollect initFactory()
    {
        program = new SprogramImpl(xProgram.getName());
        List<XOp> sInstructions = xProgram.getInstructions();
        if (sInstructions == null || sInstructions.isEmpty()) {
            throw new IllegalArgumentException("Invalid program: no instructions defined.");
        }
        instructionCollect collection = new instructionCollect(sInstructions);
        collection.extractLabels();

        xFunctions = xProgram.getFunctions();

        return collection;
    }

    // Validates that all labels referenced in jump instructions exist in the program
    private void validateLabels(List<XOp> sInstructions, LinkedHashSet<Label> definedLabels)  throws IllegalArgumentException {
        // Check all instruction arguments for label references that are not defined
        for (XOp inst : sInstructions) {
            ArrayList<XOpArguments> args = (ArrayList<XOpArguments>) inst.getArguments();
            if (args == null) continue; // if there are no arguments, skip
            for (XOpArguments arg : args) {
                String argName = arg.getName();
                String argValue = arg.getValue();
                // If this argument is a jump target label (argument name ends with "Label"):
                if (argName.toLowerCase().endsWith("label")) {
                    if ("EXIT".equalsIgnoreCase(argValue)) {
                        // "EXIT" is considered a special target (program termination), skip existence check
                        continue;
                    }
                    if (!definedLabels.contains(new LabelImpl(argValue))) {
                        // Found a jump to a label that doesn't exist in the program
                        throw new IllegalArgumentException("Invalid program: jump to undefined label \""
                                + argValue + "\" in instruction \""
                                + inst.getName() + "\".");
                    }
                }
            }
        }
    }

    private int extractFunctions( String userName, String programName) throws IllegalArgumentException
    {
        if (xFunctions == null)
            return ERROR_CODES.ERROR_OK;
        int res = ERROR_CODES.ERROR_OK;;

        xFunctions.forEach(xFunc -> {
            String name = xFunc.getName();
            FunctionExecutorImpl sFunc = ProgramCollection.getFunction(name);
            if (sFunc == null) {
                sFunc = new FunctionExecutorImpl(name);
                sFunc.setUserString(xFunc.getUserString());
                ProgramCollection.registerFunction(userName, name, programName, sFunc);
                functionNamesOfProgram.add(name);
            }
            functions.add(name);
        });

        for (XFunction xFunc : xFunctions) {
            String name = xFunc.getName();

            if (ProgramCollection.isToUpdateFunction(name)) {
                List<XOp> sInstructions = xFunc.getInstructions();
                if (sInstructions == null || sInstructions.isEmpty()) {
                    throw new IllegalArgumentException("Invalid function: " + xFunc.getName() + " no instructions defined.");
                }
                instructionCollect collection = new instructionCollect(sInstructions);
                collection.extractLabels();

                FunctionExecutor func = ProgramCollection.getFunction(xFunc.getName());

                validateLabels(collection.getInstructions(), collection.getDefinedLabels());
                res = buildProgram(func, collection.getInstructions(), collection.getDefinedLabels());
                if (res != ERROR_CODES.ERROR_OK)
                    break; // stop loop on first error
                ProgramCollection.updateFunctionStatistics(name);
            }
        };
        return res;
    }

    private int validateFunctions(String funcName, String funcArgs) {

        if (!ProgramCollection.isFunctionExists(funcName))
            return ERROR_CODES.ERROR_FUNCTION_MISSING;
        functions.add(funcName);

        if (funcArgs == null || !funcArgs.contains("(")) {
            return ERROR_CODES.ERROR_OK;
        }

        int i = 0;
        int len = funcArgs.length();
        while (true) {
            String word = "";
            // Find next '('
            int start = funcArgs.indexOf('(', i);
            if (start == -1) break;

            // Move to first character after '('
            int pos = start + 1;

            while (pos < len && Character.isWhitespace(funcArgs.charAt(pos))) {
                pos++;
            }

            // if next char is '(' then there's no word right after this '('
            if (pos < len && funcArgs.charAt(pos) == '(') {
                i = start + 1; // continue search after this '('
                continue;
            }

            // Extract a word until ',' or ')' or whitespace
            int wordStart = pos;
            while (pos < len &&
                    funcArgs.charAt(pos) != ',' &&
                    funcArgs.charAt(pos) != ')' &&
                    !Character.isWhitespace(funcArgs.charAt(pos))) {
                pos++;
            }

            if (wordStart < pos) {
                word = funcArgs.substring(wordStart, pos);
                if (!ProgramCollection.isFunctionExists(word))
                    return ERROR_CODES.ERROR_FUNCTION_MISSING;
                functions.add(word);
            }

            // continue searching for the next '(' after current '('
            i = start + 1;
        }

        return ERROR_CODES.ERROR_OK;
    }

    // 4. Build internal Program object
    public int buildProgram(FunctionExecutor program, List<XOp> sInstructions, LinkedHashSet<Label> definedLabels ) throws IllegalArgumentException
    {
        // Prepare a set to track all variable names used (for initialization)
        Set<VariableImpl> allVars = new HashSet<>();
        Set<VariableImpl> inputVars = new TreeSet<>(
                Comparator.comparing(VariableImpl::getRepresentation)
        );
        Label lbl;
        String labelRegex = "L\\d+"; // regex pattern for valid labels like L1, L2, etc.
        int i = 1;

        // Convert each SInstruction to an AbstractOpBasic object and add to program
        for (XOp inst : sInstructions) {
            String cmdName = inst.getName();    // e.g., INCREASE, ZERO_VARIABLE, etc.
            String varName = inst.getVariable();  // e.g., "x1", "y", "z2"
            String labelName = inst.getLabel();   // may be null
            int varIndex;

            if (labelName == null || labelName.equals( FixedLabel.EMPTY.getLabelRepresentation() )|| labelName.isEmpty())
                lbl = FixedLabel.EMPTY;
           else  if (labelName.equals( FixedLabel.EXIT.getLabelRepresentation()))
            {
             lbl = FixedLabel.EXIT;
            }
            else
                lbl = new LabelImpl(labelName);

            // Add the main variable to the set of variables
            if (varName == null || varName.isEmpty())
                throw new IllegalArgumentException("Instruction missing variable: " + cmdName);
            if(!varName.equals("y"))
                varIndex = Integer.parseInt(varName.substring(1)); // extract index after first char
            else
                varIndex = 0; // for result variable "y", index is 0
            VariableType vType = varName.equals("y") ? VariableType.RESULT :
                    (varName.startsWith("x") ? VariableType.INPUT : VariableType.WORK);
            VariableImpl curVar = new VariableImpl(vType, varIndex);


            allVars.add(curVar);  // track this variable for initialization
            if(// if it's an input variable, track in inputVars list too
                    vType.equals( VariableType.INPUT))  {
                inputVars.add(curVar);
            }

            AbstractOpBasic op;  // will point to a new instruction object
            // Determine which specific AbstractOpBasic subclass to instantiate based on the command name
            switch (cmdName) {
                case "INCREASE":
                    op = new OpIncrease(curVar, lbl);
                    break;
                case "DECREASE":
                    op = new OpDecrease(curVar, lbl);
                    break;
                case "NEUTRAL":
                    op = new OpNeutral(curVar, lbl);
                    break;
                case "JUMP_NOT_ZERO": {
                    // JumpNotZero needs the target label to jump to if variable != 0
                    String targetLabelName = getArgumentValue(inst, "JNZLabel");
                    Label targetLabel;
                    if (targetLabelName.equals( FixedLabel.EXIT.getLabelRepresentation())) {
                        targetLabel = FixedLabel.EXIT;
                    } else if (targetLabelName.equals(FixedLabel.EMPTY.getLabelRepresentation())) {
                       targetLabel = FixedLabel.EMPTY;
                    }
                    else
                        targetLabel = new LabelImpl(Integer.parseInt(targetLabelName.substring(1)));

                    op = new OpJumpNotZero(curVar,targetLabel,lbl);
                    break;
                }
                case "JUMP_ZERO": {
                    String targetLabelName = getArgumentValue(inst, "JZLabel");
                    Label targetLabel;
                    if (targetLabelName.equals( FixedLabel.EXIT.getLabelRepresentation())) {
                        targetLabel = FixedLabel.EXIT;
                    } else if (targetLabelName.equals(FixedLabel.EMPTY.getLabelRepresentation())) {
                        targetLabel = FixedLabel.EMPTY;
                    }
                    else
                        targetLabel = new LabelImpl(Integer.parseInt(targetLabelName.substring(1)));
                    op = new OpJumpZero(curVar, lbl, targetLabel);
                    break;
                }
                case "GOTO_LABEL": {
                    String targetLabelName = getArgumentValue(inst, "gotoLabel");
                    Label targetLabel;
                    if (targetLabelName.equals( FixedLabel.EXIT.getLabelRepresentation())) {
                        targetLabel = FixedLabel.EXIT;
                    } else if (targetLabelName.equals(FixedLabel.EMPTY.getLabelRepresentation())) {
                        targetLabel = FixedLabel.EMPTY;
                    }
                    else
                        targetLabel = new LabelImpl(Integer.parseInt(targetLabelName.substring(1)));

                    op = new OpGoToLabel(curVar, lbl, targetLabel);
                    break;
                }
                case "ASSIGNMENT": {
                    // Assignment: copies one variable's value to another
                    String srcVarName = getArgumentValue(inst, "assignedVariable");
                    VariableImpl srcVar = new VariableImpl(srcVarName.equals("y") ? VariableType.RESULT :
                            (srcVarName.startsWith("x") ? VariableType.INPUT : VariableType.WORK), Integer.parseInt(srcVarName.substring(1)))
                    ;
                    allVars.add(srcVar);  // source variable also involved
                    if(// if it's an input variable, track in inputVars list too
                            srcVar.getType() == VariableType.INPUT && !inputVars.contains(curVar)) {
                        inputVars.add(srcVar);
                    }
                    op = new OpAssignment(curVar, lbl, srcVar);
                    break;
                }
                case "CONSTANT_ASSIGNMENT": {
                    String constValStr = getArgumentValue(inst, "constantValue");
                    Long constVal = Long.parseLong(constValStr);
                    op = new OpConstantAssigment(curVar, lbl, constVal);
                    break;
                }
                case "JUMP_EQUAL_CONSTANT": {
                    String targetLabelName = getArgumentValue(inst, "JEConstantLabel");
                    String constValStr = getArgumentValue(inst, "constantValue");
                    long constVal;
                    try {
                        constVal = Long.parseLong(constValStr);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid constant value: " + constValStr
                                + " (expected a valid integer)");
                    }
                    Label targetLabel;
                    if (targetLabelName.equals( FixedLabel.EXIT.getLabelRepresentation())) {
                        targetLabel = FixedLabel.EXIT;
                    } else if (targetLabelName.equals(FixedLabel.EMPTY.getLabelRepresentation())) {
                        targetLabel = FixedLabel.EMPTY;
                    }
                    else
                        targetLabel = new LabelImpl(Integer.parseInt(targetLabelName.substring(1)));
                    op = new OpJumpEqualConstant(curVar, lbl, targetLabel, constVal);

                    break;
                }
                    case "JUMP_EQUAL_VARIABLE": {
                        String targetLabelName = getArgumentValue(inst, "JEVariableLabel");
                        String otherVarName = getArgumentValue(inst, "variableName");
                        VariableImpl otherVar = new VariableImpl(otherVarName.equals("y") ? VariableType.RESULT :
                                (otherVarName.startsWith("x") ? VariableType.INPUT : VariableType.WORK), Integer.parseInt(otherVarName.substring(1)))
                                ;
                        allVars.add(otherVar);  // second variable used in comparison
                        if(// if it's an input variable, track in inputVars list too
                                otherVar.getType() == VariableType.INPUT && !inputVars.contains(curVar)) {
                            inputVars.add(otherVar);
                        }
                        Label targetLabel;
                        if (targetLabelName.equals( FixedLabel.EXIT.getLabelRepresentation())) {
                            targetLabel = FixedLabel.EXIT;
                        } else if (targetLabelName.equals(FixedLabel.EMPTY.getLabelRepresentation())) {
                            targetLabel = FixedLabel.EMPTY;
                        }
                        else
                            targetLabel = new LabelImpl(Integer.parseInt(targetLabelName.substring(1)));
                        op = new OpJumpEqualVariable(curVar, lbl, targetLabel, otherVar);
                        break;
                    }
                    case "ZERO_VARIABLE":
                        op = new OpZeroVariable(curVar, lbl);
                        break;
                    case "JUMP_EQUAL_FUNCTION":
                    {
                        String targetLabelName = getArgumentValue(inst, "JEFunctionLabel");
                        String funcName = getArgumentValue(inst, "functionName");
                        String functionArguments = getArgumentValue(inst, "functionArguments");

                        if (validateFunctions(funcName, functionArguments) != ERROR_CODES.ERROR_OK)
                            return ERROR_CODES.ERROR_FUNCTION_MISSING;

                        Label targetLabel;
                        if (targetLabelName.equals( FixedLabel.EXIT.getLabelRepresentation())) {
                            targetLabel = FixedLabel.EXIT;
                        } else if (targetLabelName.equals(FixedLabel.EMPTY.getLabelRepresentation())) {
                            targetLabel = FixedLabel.EMPTY;
                        }
                        else
                            targetLabel = new LabelImpl(Integer.parseInt(targetLabelName.substring(1)));
                        extractVarFromArgs( functionArguments,  inputVars,  allVars);
                        op = new OPJumpEqualFunction(curVar, lbl, funcName, functionArguments, targetLabel);
                        break;
                    }
                    case "QUOTE": {
                        String funcName = getArgumentValue(inst, "functionName");
                        String functionArguments = getArgumentValue(inst, "functionArguments");
                        if (validateFunctions(funcName, functionArguments) != ERROR_CODES.ERROR_OK)
                            return ERROR_CODES.ERROR_FUNCTION_MISSING;
                        extractVarFromArgs( functionArguments,  inputVars,  allVars);
                        op = new OPQuote(curVar, lbl, funcName, functionArguments);
                        break;
                    }
                    default:
                        throw new IllegalArgumentException("Unknown instruction name: " + cmdName);
                }
                if (lbl != FixedLabel.EXIT && lbl != FixedLabel.EMPTY) {
                    program.addLabel(lbl, op);
                }

                i++;

                program.getOps().add(op);  // add the constructed operation to the program's list
            }
            // Also ensure the special result variable "y" exists and is initialized to 0
        allVars.add(VariableImpl.RESULT);
        program.addLabelSet(definedLabels);
        //sort input vars by there get representation method
        program.setInputVars(inputVars);
        program.setAllVars(allVars);
        program.init();

        return ERROR_CODES.ERROR_OK;
    }

    public static boolean isXYZThenNumber(String theString) {
        return theString != null && theString.matches("^[xyz]\\d+$");
    }

    private void extractVarFromArgs(String functionArguments, Set<VariableImpl> inputVars, Set<VariableImpl> allVars) {
        // this function extracts variable names from a comma-separated argument string

        List<String> args = Arrays.stream(functionArguments.split(","))
                .map(String::trim)
                .toList();
        for (String arg : args) {
            String cleanArg = arg.replace("(", "").replace(")", "");
            if (isXYZThenNumber(cleanArg)) {
                VariableImpl tmpVar = new VariableImpl(cleanArg);
                if (cleanArg.startsWith("x")) {
                    inputVars.add(tmpVar);
                }
                allVars.add(tmpVar);
            }
        }
    }


    private String getArgumentValue(XOp inst, String argName) {
        ArrayList<XOpArguments> args = (ArrayList<XOpArguments>) inst.getArguments();
        if (args == null) {
            throw new IllegalArgumentException("Instruction \"" + inst.getName()
                    + "\" is missing argument: " + argName);
        }
        for (XOpArguments arg : args) {
            if (argName.equals(arg.getName())) {
                return arg.getValue();
            }
        }
        // If not found by name, throw an error
        throw new IllegalArgumentException("Instruction \"" + inst.getName()
                + "\" is missing argument: " + argName);
    }
}



