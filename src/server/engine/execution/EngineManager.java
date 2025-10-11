package server.engine.execution;

import server.auth.UserManager;
import server.engine.input.XmlTranslator.Factory;

import java.io.InputStream;

public class EngineManager {
    private static Factory factory = new Factory();

    public static int addProgram(String userName, String fileName, InputStream xmlStream) {
        try {
            if (ProgramCollection.isProgramExists(fileName)) {
                return ERROR_CODES.ERROR_PROGRAM_EXISTS;
            }

            int res;
            int nFunctionsBefore = ProgramCollection.getListFunctions().size();

            res = factory.loadProgramFromXml(fileName, xmlStream);

            int nFunctionsAfter = ProgramCollection.getListFunctions().size();
            if (nFunctionsAfter > nFunctionsBefore)
                UserManager.incrementFunctions(userName, nFunctionsAfter - nFunctionsBefore);

            if (res == ERROR_CODES.ERROR_OK) {
                UserManager.incrementPrograms(userName);
            } else if (res == ERROR_CODES.ERROR_FUNCTION_MISSING) {
                return ERROR_CODES.ERROR_FUNCTION_MISSING;
            }

            return ERROR_CODES.ERROR_OK;
        } catch (Exception e) {
            e.printStackTrace();
            return ERROR_CODES.ERROR_INVALID_FILE;
        }
    }
}
