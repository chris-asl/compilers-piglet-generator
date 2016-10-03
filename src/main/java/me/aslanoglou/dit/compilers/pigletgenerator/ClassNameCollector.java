package me.aslanoglou.dit.compilers.pigletgenerator;

import me.aslanoglou.dit.compilers.pigletgenerator.syntaxtree.*;
import me.aslanoglou.dit.compilers.pigletgenerator.visitor.GJDepthFirst;

import java.util.*;

public class ClassNameCollector extends GJDepthFirst<String, String>{
    Set<String> verifiedClassNames;
    Map<String, List<String>> superClassRelation;

    public ClassNameCollector() {
        verifiedClassNames = new HashSet<>();
        superClassRelation = new HashMap<>();
    }

    //**********************************************//
    // Collecting all ClassNames
    //**********************************************//
    @Override
    public String visit(ClassDeclaration n, String argu) {
        String className;
        className = n.f1.accept(this, argu);
        // Check reDeclaration
        if (verifiedClassNames.contains(className))
            throw new RuntimeException("Re-declaration of Class " + className);
        verifiedClassNames.add(className);
        return className;
    }


    public ArrayList<String> populateSuperClassSet(String superClass) {
        ArrayList<String> tmp = new ArrayList<>();
        tmp.add(superClass);
        if (superClassRelation.containsKey(superClass)) {
            // Add more superclasses
            tmp.addAll(superClassRelation.get(superClass));
        }
        return tmp;
    }

    @Override
    public String visit(ClassExtendsDeclaration n, String argu) {
        String ret = "";
        String className = n.f1.accept(this, argu);
        // Check reDeclaration
        if (verifiedClassNames.contains(className))
            throw new RuntimeException("Re-declaration of Class " + className);
        else
            verifiedClassNames.add(className);
        // Check that the superclass is already defined and it's not the mainClass
        String superClassName = n.f3.accept(this, argu);
        if (!verifiedClassNames.contains(superClassName))
            throw new RuntimeException("Superclass: " + superClassName + " isn't already defined");
        // Add superclass relation
        superClassRelation.put(className, populateSuperClassSet(superClassName));
        return ret;
    }

    @Override
    public String visit(MainClass n, String argu) {
        String className;
        className = n.f1.accept(this, argu);
        // Check reDeclaration
        if (verifiedClassNames.contains(className))
            throw new RuntimeException("Re-declaration of Class " + className);
        verifiedClassNames.add(className);
        return className;
    }
    //**********************************************//

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
