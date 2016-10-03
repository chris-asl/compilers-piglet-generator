package me.aslanoglou.dit.compilers.pigletgenerator;

import me.aslanoglou.dit.compilers.pigletgenerator.containers.*;
import me.aslanoglou.dit.compilers.pigletgenerator.syntaxtree.*;
import me.aslanoglou.dit.compilers.pigletgenerator.visitor.GJDepthFirst;

import java.util.*;

public class ClassMembersVisitor extends GJDepthFirst<String, String> {
    Set<String> verifiedClassNames;
    Map<String, List<String>> superClassRelation;
    Map<String, ClassContainer> classInfo;

    public ClassMembersVisitor(Set<String> verifiedClassNames, Map<String,
            List<String>> superClassRelation) {
        this.verifiedClassNames = verifiedClassNames;
        classInfo = new HashMap<>();
        this.superClassRelation = superClassRelation;
    }

    // Accept all Classes and start collecting fields and methods
    public String visit(ClassDeclaration n, String argu) {
        // get Class name
        String className = n.f1.accept(this, argu);
        ClassContainer classContainer = new ClassContainer(className, null);
        if (className.equals("Visitor"))
            System.out.println("In class Visitor");
        // Collect fields
        if (n.f3.present()) {
            populateFields(n.f3, classContainer);
        }
        // Collect methods info
        if (n.f4.present()) {
            populateMethods(n.f4, classContainer);
        }
        // Add to classInfo
        classInfo.put(className, classContainer);
        return className;
    }

    public String visit(ClassExtendsDeclaration n, String argu) {
        // get Class name
        String className = n.f1.accept(this, argu);
        ClassContainer classContainer = new ClassContainer(className, superClassRelation.get(className));
        String superClassName = n.f3.accept(this, argu);
        ClassContainer superClass = classInfo.get(superClassName);
        classContainer.populateSupersMembers(superClass);
        // Collect fields
        if (n.f5.present()) {
            populateFields(n.f5, classContainer);
        }
        // Collect methods info
        if (n.f6.present()) {
            populateMethods(n.f6, classContainer);
        }
        // Add to classInfo
        classInfo.put(className, classContainer);
        return className;
    }

    public String visit(MainClass n, String argu) {
        // get Class name
        String className = n.f1.accept(this, argu);
        ClassContainer classContainer = new ClassContainer(className, null);
        // Populate method info
        FunctionContainer function = new FunctionContainer("main", "void", className, 0);
        // Ignore String[] array argument
        // Add local vars
        if (n.f14.present()) {
            Enumeration<Node> varsEnum = n.f14.elements();
            StringBuilder varsB = new StringBuilder();
            while(varsEnum.hasMoreElements()) {
                VarDeclaration varDecl = (VarDeclaration) varsEnum.nextElement();
                varsB.append(varDecl.f0.accept(this, argu) + " " + varDecl.f1.accept(this, argu) + ", ");
            }
            varsB.delete(varsB.length() - 2, varsB.length() - 1);
            String[] vars = varsB.toString().split(",");
            for (int i = 0; i < vars.length; i++) {
                String[] variableEntry = vars[i].split(" ");
                function.addVar(variableEntry[1].trim(), variableEntry[0].trim());
            }
        }
        classContainer.getFunctions().put("main", function);
        // Add to classInfo
        classInfo.put(className, classContainer);
        return className;
    }


    /**
     * Responsible for checking the validity of the types of a variable or a method
     * @param type The String that needs to be checked
     * @return True if the type is valid, else false
     */
    public boolean isValidType(String type) {
        return (verifiedClassNames.contains(type) || type.equals("int") || type.equals("boolean") || type.equals("int[]"));
    }

    /**
     * Function responsible for populating the fields field of a ClassContainer Object
     * Also, checks that a field has to be unique, if not it throws an exception
     * @param fieldsListOptional a NodeListOptional field containing the fields of the function
     * @param classContainer a ClassContainer that its fields field will be populated
     */
    public void populateFields(NodeListOptional fieldsListOptional, ClassContainer classContainer) {
        Enumeration<Node> fields = fieldsListOptional.elements();
        while(fields.hasMoreElements()) {
            Node node = fields.nextElement();
            // Type - checking
            String type = node.accept(this, "type");
            if (!isValidType(type))
                throw new RuntimeException("Cannot resolve symbol '"+ type + "'");
            String name = node.accept(this, "name");
            VariableContainer field = new VariableContainer(name, type, classContainer.getClassName(), classContainer.getFields().size() + 1);
            if (classContainer.getFields().containsKey(name) &&
                    (classContainer.getFields().get(name).getClassName().equals(classContainer.getName())))
                throw new RuntimeException("Variable '" + name + "' is already defined in the scope");
            classContainer.addField(field);
        }
    }


    /**
     * Function responsible for checking if a function that will Override another one from its superClass, has
     * the same signature as the one it overrides
     * On error, RuntimeException is thrown
     */
    public boolean isOverriddenProperly(String className, FunctionContainer function, StringBuilder error) {
        // check if class has superClasses
        if (superClassRelation.containsKey(className)) {
            // Get superClasses List
            ArrayList<String> superClasses = (ArrayList<String>) superClassRelation.get(className);
            // Iterate through all the superClasses to check for correct Overriding
            for (int i = 0; i < superClasses.size(); i++) {
                ClassContainer superClass = classInfo.get(superClasses.get(i));
                // Check if superClass has this function defined
                if (superClass.getFunctions().containsKey(function.getName())) {
                    // Checking if the signatures of the two methods are identical
                    if (!superClass.getFunctions().get(function.getName()).isIdentical(function)) {
                       error.append("'" + className + "." + function.getName() + "' clashes with '"
                               + superClass.getName() + "." + function.getName() + "'");
                       return false;
                    }
                }
            }
        }
        return true;
    }


    /**
     * Function responsible for populating the Methods field of a ClassContainer object
     * Also, checks for methods' uniqueness
     * @param methodsListOptional The methods field in the syntax tree
     * @param classContainer The ClassContainer object that its methods field is populated
     */
    public void populateMethods(NodeListOptional methodsListOptional, ClassContainer classContainer) {
        Enumeration<Node> methods = methodsListOptional.elements();
        while(methods.hasMoreElements()) {
            Node node = methods.nextElement();
            // verify that returnType is correct
            String returnType = node.accept(this, "type");
            if (!isValidType(returnType))
                throw new RuntimeException("Cannot resolve symbol '" + returnType + "'");
            String identifier = node.accept(this, "id");
            // check if there is already a function defined with the same name
            if (classContainer.getFunctions().containsKey(identifier) &&
                    (classContainer.getFunctions().get(identifier).getClassName().equals(classContainer.getName()))) {
                // prepare error message
                String error = "'" + identifier + "(...)' is already defined in '" + classContainer.getName() + "'";
                throw new RuntimeException(error);
            }
            FunctionContainer function = new FunctionContainer(identifier, returnType, classContainer.getName(), classContainer.getFunctions().size() + 1);
            // Get formalParameters
            String params = node.accept(this, "params");
            if (!params.equals("")) {
                // De-serialize params string
                String[] othParams = params.split(",");
                for (int i = 0; i < othParams.length; i++) {
                    othParams[i] = othParams[i].trim();
                    String[] paramDeclaration = othParams[i].split(" ");
                    function.addParam(paramDeclaration[1], paramDeclaration[0]);
                }
            }
            // Check Override correctness
            StringBuilder error = new StringBuilder();
            if (!isOverriddenProperly(classContainer.getName(), function, error)) {
                throw new RuntimeException(error.toString());
            }
            // Add local vars
            String vars = node.accept(this, "vars");
            if (!vars.equals("")) {
                // De-serialize vars string
                String[] othVars = vars.split(",");
                for (int i = 0; i < othVars.length; i++) {
                    othVars[i] = othVars[i].trim();
                    String[] varDeclaration = othVars[i].split(" ");
                    // remove trailing comma
                    varDeclaration[1] = varDeclaration[1].substring(0, (varDeclaration[1]).length());
                    function.addVar(varDeclaration[1], varDeclaration[0]);
                }
            }
            classContainer.addFunction(function);
        }
    }

    public String visit(VarDeclaration n, String argu) {
        if (argu.equals("name"))
            return n.f1.accept(this, argu);
        else if (argu.equals("type"))
            return n.f0.accept(this, argu);
        else
            return argu;
    }

    //************************************//
    //******** Method Declaration ********//
    public String visit(MethodDeclaration n, String argu) {
        if (argu.equals("type"))
            return n.f1.accept(this, argu);
        else if (argu.equals("id"))
            return n.f2.accept(this, argu);
        else if (argu.equals("params")){
            // Check if params exist
            if (n.f4.present()){
                // Params String will be in this form (delimiter ',')
                // [ paramType paramName ]*
                StringBuilder params = new StringBuilder();
                String firstParam = n.f4.accept(this, "first");
                params.append(n.f4.accept(this, "oth"));
                return firstParam + params.toString();
            }
            else
                return "";
        }
        else if (argu.equals("vars")) {
            // Check if vars exist
            if (n.f7.present()){
                // Vars String will be in this form (delimiter ',')
                // [varType varName ]*
                Enumeration<Node> fields = n.f7.elements();
                StringBuilder vars = new StringBuilder();
                while(fields.hasMoreElements()) {
                    Node node = fields.nextElement();
                    // Type - checking
                    String type = node.accept(this, "type");
                    if (!isValidType(type))
                        throw new RuntimeException("Cannot resolve symbol '"+ type + "'");
                    String name = node.accept(this, "name");
                    vars.append(type + " " + name + ",");
                }
                return vars.toString();
            }
            else
                return "";
        }
        else
            return "WUT?!";
    }

    public String visit(FormalParameterList n, String argu) {
        if (argu.equals("first"))
            return n.f0.accept(this, null);
        else if (argu.equals("oth")){
            return n.f1.accept(this, null);
        }
        else
            return argu;
    }

    public String visit(FormalParameter n, String argu) {
        String type = n.f0.accept(this, argu);
        if (!isValidType(type))
            throw new RuntimeException("Cannot resolve symbol '" + type + "'");
        String name = n.f1.accept(this, argu);
        return type + " " + name;
    }

    public String visit(FormalParameterTail n, String argu) {
        // check if there are more params
        if (n.f0.present()) {
            Enumeration<Node> otherParams = n.f0.elements();
            StringBuilder otherParamsStr = new StringBuilder();
            while(otherParams.hasMoreElements()){
                Node tmp = otherParams.nextElement();
                otherParamsStr.append(", ").append(tmp.accept(this, argu));
            }
            return otherParamsStr.toString();
        }
        else
            return "";
    }


    public String visit(FormalParameterTerm n, String argu) {
        return n.f1.accept(this, argu);
    }
    //************************************//

    //**********************************************//
    // Returning the correct type
    //**********************************************//
    @Override
    public String visit(Type n, String argu) {
        return n.f0.accept(this, argu);
    }

    @Override
    public String visit(ArrayType n, String argu) {
        return "int[]";
    }

    @Override
    public String visit(BooleanType n, String argu) {
        return "boolean";
    }

    @Override
    public String visit(IntegerType n, String argu) {
        return "int";
    }

    @Override
    public String visit(Identifier n, String argu) {
        return n.f0.withSpecials();
    }
    //**********************************************//





}
