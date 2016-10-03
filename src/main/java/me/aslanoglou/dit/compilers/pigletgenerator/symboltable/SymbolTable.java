package me.aslanoglou.dit.compilers.pigletgenerator.symboltable;

import me.aslanoglou.dit.compilers.pigletgenerator.containers.ClassContainer;
import me.aslanoglou.dit.compilers.pigletgenerator.containers.VariableContainer;

import java.util.*;

/**
 * Captures data of the source file, such as subtyping relations, classesInfo while providing lookup facility.
 */
public class SymbolTable {
    private Set<String> verifiedClassNames;
    private Map<String, List<String>> superClassRelation;
    private Map<String, ClassContainer> classInfo;

    // Actual symbol table structure
    private ArrayList<Map<String, VariableContainer>> symbolTable;

    public SymbolTable(Set<String> verifiedClassNames,
                       Map<String, List<String>> superClassRelation,
                       Map<String, ClassContainer> classInfo) {
        this.verifiedClassNames = verifiedClassNames;
        this.superClassRelation = superClassRelation;
        this.classInfo = classInfo;
        symbolTable = new ArrayList<>();
    }

    public ClassContainer getClass(String className) {
        if (classInfo.containsKey(className))
            return classInfo.get(className);
        return null;
    }

    public Map<String, List<String>> getSuperClassRelation() {
        return superClassRelation;
    }

    public List<String> getSuperClasses(String className) {
        return superClassRelation.get(className);
    }

    public void print() {
        System.out.println("========== Symbol Table ==========");
        for (int i = 0; i < symbolTable.size(); i++) {
            System.out.println("__________________________________");
            System.out.println("Scope: " + i);
            Iterator it = symbolTable.get(i).entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry STEntry = (Map.Entry) it.next();
                System.out.println(STEntry.getKey() + "\t->\t"
                        + ((VariableContainer)STEntry.getValue()).getType());
            }
        }
        System.out.println("==================================");
    }

    // Symbol Table interface
    public boolean isEmpty() {
        return symbolTable.size() == 0;
    }

    // Leaves a scope level
    public void exit() {
        symbolTable.remove(symbolTable.size() - 1);
    }

    // Creates a new scope level
    public void enter() {
        symbolTable.ensureCapacity(symbolTable.size());
        symbolTable.add(new HashMap<String, VariableContainer>());
    }

    // Inserts at the current (latest) level
    public void insert(String varName, VariableContainer variable){
        if (this.isEmpty())
            enter();
        symbolTable.get(symbolTable.size() - 1).put(varName, variable);
    }

    // Looks for a variable into the SymbolTable
    // If there's no related info, null is returned
    public VariableContainer lookup(String varName) {
        for (int i = 0; i < symbolTable.size(); i++) {
            VariableContainer variable = lookupHere(varName, i);
            if (variable != null)
                return variable;
        }
        // if it's not found up until now, maybe it's a class, so global scope
        if (classInfo.containsKey(varName))
            return new VariableContainer(varName, "class " + varName, varName, -1);
        else
            return null;
    }


    private VariableContainer lookupHere(String varName, int idx){
        if (symbolTable.get(idx).containsKey(varName))
            return symbolTable.get(idx).get(varName);
        else
            return null;
    }

    public ClassContainer getSuperClass(String className) {
        // check if it has superClasses
        List<String> supers = superClassRelation.get(className);
        if (supers == null)
            return null;
        else {
            // Return the 1st one
            return classInfo.get(supers.get(0));
        }
    }
}
