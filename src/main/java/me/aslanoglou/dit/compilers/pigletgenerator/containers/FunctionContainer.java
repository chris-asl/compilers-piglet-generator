package me.aslanoglou.dit.compilers.pigletgenerator.containers;

import java.util.*;

/**
 * Holds information for functions.
 */
public class FunctionContainer extends Container{
    // ParamName -> ParamType
    private Map<String, VariableContainer> params;
    // VarName -> VarType
    private Map<String, VariableContainer> vars;
    private boolean isOverridden;

    public FunctionContainer(String name, String type, String className, int index) {
        super(name, type, className, index, true);
        params = new HashMap<>();
        vars = new HashMap<>();
        isOverridden = false;
    }

    public Map<String, VariableContainer> getParams() {
        return params;
    }

    public Map<String, VariableContainer> getVars() {
        return vars;
    }

    @Override
    public String toString() {
        StringBuilder paramsStr = new StringBuilder();
        if (params.size() != 0) {
            Iterator it = params.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry paramEntry = (Map.Entry) it.next();
                VariableContainer param = (VariableContainer) paramEntry.getValue();
                paramsStr.append(param.getType() + " " + param.getName() + ", ");
            }
            paramsStr.delete(paramsStr.length() - 2, paramsStr.length());
        }
        StringBuilder varsStr = new StringBuilder();
        if (vars.size() != 0) {
            Iterator it = vars.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry varEntry = (Map.Entry) it.next();
                VariableContainer var = (VariableContainer) varEntry.getValue();
                varsStr.append("\t" + var.getType() + " " + var.getName() + ";\n");
            }
            varsStr.delete(varsStr.length() - 1, varsStr.length());
        }
        return " [" + this.order + "] " + type + " "  + name + "(" + paramsStr.toString() + ") { " + varsStr.toString() + " }\n";
    }

    public boolean isIdentical(FunctionContainer function) {
        // Check if return type is the same
        if (!this.type.equals(function.type))
            return false;
        else {
            // Check formalParameters, but firstly check their number
            if (this.params.size() != function.params.size())
                return false;
            Iterator it = params.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry paramEntry = (Map.Entry) it.next();
                // Extract param from current and given
                VariableContainer param = (VariableContainer) paramEntry.getValue();
                // Check if it exists
                if (!function.params.containsKey(param.name))
                    return false;
                VariableContainer paramOther = (VariableContainer) function.params.get(param.name);
                if (!param.isIdenticalOrdered(paramOther))
                    return false;
            }
            this.isOverridden = true;
            return true;
        }
    }


    public void addParam(String paramName, String paramType) {
        if (params.containsKey(paramName))
            throw new RuntimeException("Variable '" + paramName + "' is already defined in this scope");
        VariableContainer param = new VariableContainer(paramName, paramType, this.className, this.params.size() + 1);
        params.put(paramName, param);
    }

    public void addVar(String VarName, String VarType) {
        if (vars.containsKey(VarName))
            throw new RuntimeException("Variable '" + VarName + "' is already defined in this scope");
        VariableContainer var = new VariableContainer(VarName, VarType, this.className, this.vars.size() + 1);
        vars.put(VarName, var);
    }

    // responsible for returning the VariableContainer that represents the i-th parameter
    public VariableContainer getParam(int i) {
        for(String varName: params.keySet()) {
            VariableContainer variable = params.get(varName);
            if (variable.getOrder() == i)
                return variable;
        }
        return null;
    }

    public int getParamsSize() {
        return params.size();
    }

    public String getVarType(String identifier) {
        if (params.containsKey(identifier))
            return params.get(identifier).getType();
        else if (vars.containsKey(identifier))
            return vars.get(identifier).getType();
        else
            return null;
    }

    public boolean belongsInLocals(String identifier) {
        return params.containsKey(identifier) || vars.containsKey(identifier);
    }

    // Not needed in 3rd assignment
//    public void checkCorrectParams(String[] args, Map<String, List<String>> classInfo) {
//        // check equal number
//        if (args[0].equals(""))
//            return;
//        if (args.length != params.size()){
//            String error = "ParametersNumber - Expected: " + params.size() + " Found: " + args.length;
//            throw new RuntimeException(error);
//        }
//        // Build array with the types of the parameters
//        String[] paramTypes = new String[args.length];
//        for(String paramName: params.keySet()) {
//            VariableContainer tmp = params.get(paramName);
//            paramTypes[tmp.getOrder() - 1] = tmp.getType();
//        }
//        for (int i = 0; i < args.length; i++) {
//            // check for subType
//            List<String> supers = classInfo.get(args[i]);
//            if (supers != null) {
//                if (!supers.contains(paramTypes[i])) {
//                    String error = "Incompatible types. Required '" + paramTypes[i] + "' and found: '" + args[i] + "'";
//                    throw new RuntimeException(error);
//                }
//            }
//            else
//                TypeCheckingVisitor.ensureType(paramTypes[i], args[i].trim());
//        }
//    }
}
