package me.aslanoglou.dit.compilers.pigletgenerator.containers;

import java.util.*;


/**
 * Holds information for classes.
 */
public class ClassContainer extends Container {
    // fieldName -> fieldType
    private Map<String, VariableContainer> fields;
    // functionName -> FunctionContainer
    private Map<String, FunctionContainer> functions;
    private List<String> superClasses;

    public ClassContainer(String name, List<String> supers) {
        super(name, "class", name, 0, false);
        fields = new HashMap<>();
        functions = new HashMap<>();
        superClasses = supers;
    }

    public Map<String, FunctionContainer> getFunctions() {
        return functions;
    }

    public FunctionContainer getFunction(String functionName) {
        if (this.functions.containsKey(functionName))
            return this.functions.get(functionName);
        else
            return null;
    }

    public Map<String, VariableContainer> getFields() {
        return fields;
    }

    public String getFunctionName(int idx) {
        Set<Map.Entry<String, FunctionContainer>> entries = functions.entrySet();
        Iterator<Map.Entry<String, FunctionContainer>> it = entries.iterator();
        while (it.hasNext()) {
            FunctionContainer function = it.next().getValue();
            if (idx == function.order)
                return function.getClassName() + "_" + function.name;
        }
        return
                null;
    }

    public int getFieldOrder(String fieldName) {
        int order = -1;
        VariableContainer field = fields.get(fieldName);
        if (field == null) {
            // Search again into map, by adding prefix to the
            // identifier
            // This happens because, when inserting fields from
            // superclasses to the current class, their className
            // is added as a prefix
            if (superClasses == null)
                throw new NullPointerException("Error, superClasses shouldn't be null in this case");
            else {
                // So, for all superclasses, look for the field again
                Iterator it = superClasses.iterator();
                while(it.hasNext()) {
                    String superClassName = (String) it.next();
                    // search with this term
                    String key = superClassName + "_" + fieldName;
                    if (fields.containsKey(key))
                        return fields.get(key).getOrder();
                }
                // end of search with no success, again error (just in case)
                throw new NullPointerException("Cannot find field: " + fieldName + " from supers' fields in class: " + getName()
                + "\nNot a valid miniJava program, have you tried passing it from a miniJava semantical analyzer?");
            }
        }
        else
            return fields.get(fieldName).getOrder();
    }

    public String getFieldType(String fieldName) {
        if (fields.get(fieldName) == null) {
            VariableContainer field = findField(fieldName);
            if (field == null)
                throw new NullPointerException("getFieldType: Searching fields return NULL");
            else
                return field.getType();
        }
        return fields.get(fieldName).getType();
    }

    public boolean containsField(String fieldName) {
        if (fields.get(fieldName) == null) {
            VariableContainer field = null;
            try {
                field = findField(fieldName);
            } catch (NullPointerException e) {
                // ignore it
            }
            if (field == null)
                return false;
            else
                return true;
        }
        else
            return true;
    }

    private VariableContainer findField(String fieldName) {
        VariableContainer field = fields.get(fieldName);
        if (field == null) {
            // Search again into map, by adding prefix to the
            // identifier
            // This happens because, when inserting fields from
            // superclasses to the current class, their className
            // is added as a prefix
            if (superClasses == null)
                return null;
            else {
                // For all the superclasses, look for the field again
                Iterator it = superClasses.iterator();
                while(it.hasNext()) {
                    String superClassName = (String) it.next();
                    // search with this term
                    String key = superClassName + "_" + fieldName;
                    if (fields.containsKey(key))
                        return fields.get(key);
                }
                // end of search with no success, again error (just in case)
                throw new NullPointerException("Cannot find field from supers in class");
            }
        }
        else
            return fields.get(fieldName);
    }

    public void addField(VariableContainer field) {
        fields.put(field.getName(), field);
    }

    public void populateSupersMembers(ClassContainer superClass) {
        Set<Map.Entry<String, FunctionContainer>> functions = superClass.getFunctions().entrySet();
        Iterator it = functions.iterator();
        while (it.hasNext()){
            Map.Entry tmp = (Map.Entry) it.next();
            this.functions.put((String) tmp.getKey(), (FunctionContainer) tmp.getValue());
        }
        Set<Map.Entry<String, VariableContainer>> fields = superClass.getFields().entrySet();
        it = fields.iterator();
        while (it.hasNext()){
            Map.Entry tmp = (Map.Entry) it.next();
            VariableContainer dummy = (VariableContainer) tmp.getValue();
            // Watch out - don't include prefix if already prefixed
            // That is if a variable that is going to be added is a field of superClass
            // Then don't add again its prefix
            String fieldName = (String)tmp.getKey();
            int idxOfSep = -1;
            idxOfSep = fieldName.indexOf('_');
            if (idxOfSep == -1) {
                this.fields.put(dummy.getClassName() + "_" + fieldName, dummy);
            }
            else {
                this.fields.put(fieldName, dummy);
            }
        }
    }

    public void addFunction(FunctionContainer function) {
        // check if there is another function, and if so simply edit
        // the new one's idx
        if (this.functions.containsKey(function.getName())) {
            // Case in which the function overrides
            // Update its order to match with the superClass's one
            function.setOrder(this.functions.get(function.name).getOrder());
        }
        this.functions.put(function.getName(), function);
    }

    @Override
    public String toString() {
        StringBuilder fieldsStr = new StringBuilder();
        if (fields.size() != 0) {
            Iterator it = fields.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry fieldEntry = (Map.Entry) it.next();
                VariableContainer field = (VariableContainer) fieldEntry.getValue();
                fieldsStr.append(field.getType() + " " + field.getName() + ",");
            }
            fieldsStr.delete(fieldsStr.length() - 1, fieldsStr.length());
        }
        return "ClassContainer {" +
                "name='" + name + "'" +
                ", \n\t\tfields=" + fieldsStr.toString() +
                ", \n\t\tfunctions=" + functions +
                '}';
    }
}
