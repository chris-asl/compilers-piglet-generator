package me.aslanoglou.dit.compilers.pigletgenerator;

import me.aslanoglou.dit.compilers.pigletgenerator.symboltable.SymbolTable;
import me.aslanoglou.dit.compilers.pigletgenerator.syntaxtree.*;
import me.aslanoglou.dit.compilers.pigletgenerator.visitor.GJDepthFirst;
import me.aslanoglou.dit.compilers.pigletgenerator.containers.*;

import java.util.*;

public class PigletGenerator extends GJDepthFirst<String, String> {
    private SymbolTable symbolTable;
    private ClassContainer currentClass;
    private FunctionContainer currentFunction;
    StringBuilder program;
    private Integer tempNo;
    private Integer paramNo;
    private Integer labelNo;
    // A localVariable to VirtualRegister mapping
    // Will be reset after a new function visit
    private Map<String, String> variableToRegister;

    private static final String stmtExpStart = "BEGIN\n";
    private static final String stmtExpMiddle = "RETURN ";
    private static final String stmtExpEnd = "END\n";

    private int calls;

    public PigletGenerator(Set<String> verifiedClassNames,
                           Map<String, List<String>> superClassRelation,
                           Map<String, ClassContainer> classInfo) {
        symbolTable = new SymbolTable(verifiedClassNames, superClassRelation, classInfo);
        program = new StringBuilder();
        currentClass = null;
        currentFunction = null;
        tempNo = 100;
        paramNo = 1;
        labelNo = 1;
        calls = 0;
        variableToRegister = new HashMap<>();
    }


    // ********************************************* //
    // ************* Classes *********************** //
    // ********************************************* //
    @Override
    public String visit(MainClass n, String argu) {
        //write MAIN label
        program.append("MAIN\n");
        String className = n.f1.accept(this, "ID");
        currentClass = symbolTable.getClass(className);
        // other statements
        if (n.f15.present()) {
            // Main function
            currentFunction = currentClass.getFunction("main");
            resetState();
            // Add all locals to map
            if (n.f14.present())
                n.f14.accept(this, argu);
            Enumeration<Node> stmtEnum = n.f15.elements();
            while(stmtEnum.hasMoreElements()){
                Statement stmt = (Statement) stmtEnum.nextElement();
                program.append(stmt.accept(this, argu));
            }
        }
        program.append("END\n");
        return "";
    }

    @Override
    public String visit(ClassDeclaration n, String argu) {
        // update current class
        String className = n.f1.accept(this, "ID");
        currentClass = symbolTable.getClass(className);
        // visit functions
        if (n.f4.present()) {
            Enumeration<Node> fields = n.f4.elements();
            while(fields.hasMoreElements()) {
                MethodDeclaration node = (MethodDeclaration) fields.nextElement();
                node.accept(this, argu);
            }
        }
        return "";
    }

    @Override
    public String visit(ClassExtendsDeclaration n, String argu) {
        // update current class
        String className = n.f1.accept(this, "ID");
        currentClass = symbolTable.getClass(className);
        // visit functions
        if (n.f6.present()) {
            Enumeration<Node> fields = n.f6.elements();
            while(fields.hasMoreElements()) {
                MethodDeclaration node = (MethodDeclaration) fields.nextElement();
                node.accept(this, argu);
            }
        }
        return "";
    }
    // ********************************************* //
    private void resetState() {
        variableToRegister.clear();
        paramNo = 1;
        labelNo = 1;
    }

    @Override
    public String visit(MethodDeclaration n, String argu) {
        String functionName = n.f2.accept(this, "ID");
        resetState();
        currentFunction = currentClass.getFunction(functionName);
        int numOfArgs = currentFunction.getParamsSize();
        program.append("\n" + currentClass.getName() + "_" + functionName + " [ " + (numOfArgs + 1) + " ]\n");
        program.append(stmtExpStart);
        // for all params and locals, insert them in the map variableToRegister
        if (n.f4.present())
            n.f4.accept(this, "param");
        if (n.f7.present())
            n.f7.accept(this, "");
        // Visit all statements
        if (n.f8.present()) {
            Enumeration<Node> statements = n.f8.elements();
            while(statements.hasMoreElements()) {
                Statement stmt = (Statement) statements.nextElement();
                program.append(stmt.accept(this, argu));
            }
        }
        // Get return value
        String returnExpr = n.f10.accept(this, "rvalue");
        program.append(stmtExpMiddle + returnExpr + "\n")
                .append(stmtExpEnd + "\n");
        return argu;
    }

    @Override
    public String visit(FormalParameterList n, String argu) {
        String firstParam = n.f0.accept(this, argu);
        String otherParams = n.f1.accept(this, argu);
        return firstParam + " " + otherParams + "\n";
    }

    @Override
    public String visit(FormalParameter n, String argu) {
        return n.f1.accept(this, argu);
    }

    @Override
    public String visit(FormalParameterTail n, String argu) {
        StringBuilder formParams = new StringBuilder();
        if (n.f0.present()) {
            Enumeration<Node> params = n.f0.elements();
            while (params.hasMoreElements()) {
                FormalParameterTerm param = (FormalParameterTerm) params.nextElement();
                formParams.append(" " + param.f1.accept(this, argu));
            }
        }
        return formParams.toString();
    }

    @Override
    public String visit(FormalParameterTerm n, String argu) {
        return n.f1.accept(this, argu);
    }

    @Override
    public String visit(VarDeclaration n, String argu) {
        return n.f1.accept(this, argu);
    }

    //**********************************************//
    // Visit statements
    //**********************************************//
    @Override
    public String visit(Statement n, String argu) {
        return n.f0.accept(this, argu);
    }

    @Override
    public String visit(PrintStatement n, String argu) {
        String expr = n.f2.accept(this, "rvalue");
        return "PRINT " + expr + "\n";
    }

    @Override
    public String visit(AssignmentStatement n, String argu) {
        // Distinguish lvalue from temp or mem variable
        String identifier = n.f0.accept(this, "ID");
        StringBuilder lvalue = new StringBuilder();
        // Is the identifier local or a class field
        if (variableToRegister.containsKey(identifier)) {
            String idTmp = variableToRegister.get(identifier);
            lvalue.append("MOVE " + idTmp + " ");
        }
        else {
            int offset = currentClass.getFieldOrder(identifier) * 4;
            lvalue.append("HSTORE PLUS TEMP 0 " + offset + " 0 ");
        }
        return lvalue + " " + n.f2.accept(this, "rvalue") + "\n";
    }

    @Override
    public String visit(ArrayAssignmentStatement n, String argu) {
        String rvalue = n.f5.accept(this, argu);
        // get identifier temp
        String identifier = n.f0.accept(this, "ID");
        // Temps
        String idxTmp = n.f2.accept(this, argu);
        String lengthTmp = getTemp();
        // Labels
        String upperOutOfBoundsLbl = getLabel("upperOutOfBounds");
        String correctLbl = getLabel("correct");
        StringBuilder arrayAssign = new StringBuilder();
        if (variableToRegister.containsKey(identifier)) {
            String baseAddrTmp = n.f0.accept(this, argu);
            arrayAssign.append("\n\n\nHLOAD " + lengthTmp + " " + baseAddrTmp + " 0\n")
                    // check idx validity
                    .append("CJUMP LT "+ idxTmp + " 0 " + upperOutOfBoundsLbl + "\n")
                    .append("ERROR\n")
                    .append(upperOutOfBoundsLbl + " CJUMP LT MINUS " + lengthTmp + " 1 " + idxTmp + " " + correctLbl + "\n")
                    .append("ERROR\n")
                    .append(correctLbl + " NOOP\n")
                    .append("HSTORE PLUS " + baseAddrTmp + " TIMES 4 PLUS 1 " + idxTmp + " 0 " + rvalue + "\n");
        }
        else {
            String baseAddrTmp = getTemp();
            // load baseAddr from object memory
            arrayAssign.append("HSTORE PLUS\n")
                    .append(stmtExpStart + "HLOAD " + baseAddrTmp + " PLUS TEMP 0 "
                            + (currentClass.getFieldOrder(identifier) * 4) + " 0\n")
                    .append(checkingNullityCode(baseAddrTmp, "ArrayAssign"))
                    .append(stmtExpMiddle + baseAddrTmp + "\n" + stmtExpEnd);
            // Calculate the offset and add it to the base address
            String offsetTmp = getTemp();
            arrayAssign.append(stmtExpStart)
                    // check idx validity
                    .append("HLOAD " + lengthTmp + " " + baseAddrTmp + " 0\n")
                    .append("CJUMP LT "+ idxTmp + " 0 " + upperOutOfBoundsLbl + "\n")
                    .append("ERROR\n")
                    .append(upperOutOfBoundsLbl + " CJUMP LT MINUS " + lengthTmp + " 1 " + idxTmp + " " + correctLbl + "\n")
                    .append("ERROR\n")
                    .append(correctLbl + " NOOP\n")
                            // calculate the bytes offset and return it
                    .append("MOVE " + offsetTmp + " TIMES 4 PLUS 1 " + idxTmp + "\n")
                    .append(stmtExpMiddle + offsetTmp + " " + stmtExpEnd)
                    .append("0 " + rvalue + "\n");
        }
        return arrayAssign.toString();
    }

    @Override
    public String visit(Block n, String argu) {
        StringBuilder statementsBlock = new StringBuilder();
        if (n.f1.present()) {
            Enumeration<Node> statements = n.f1.elements();
            while (statements.hasMoreElements()) {
                Statement stmt = (Statement) statements.nextElement();
                statementsBlock.append(stmt.accept(this, argu) + "\n");
            }
        }
        return statementsBlock.toString();
    }

    @Override
    public String visit(IfStatement n, String argu) {
        String endLbl = getLabel("ifEnd");
        String elseLbl = getLabel("else");
        StringBuilder ifStmt = new StringBuilder();
        ifStmt.append("CJUMP " + n.f2.accept(this, argu) + " " + elseLbl + "\n")
                .append(n.f4.accept(this, argu))
                .append("\nJUMP " + endLbl + "\n")
                .append(elseLbl + " " + n.f6.accept(this, argu))
                .append(endLbl + " NOOP\n");
        return ifStmt.toString();
    }

    @Override
    public String visit(WhileStatement n, String argu) {
        StringBuilder whileStmt = new StringBuilder();
        // Labels
        String endLbl = getLabel("loopEnd");
        String startLbl = getLabel("loopStart");
        // Code segments
        whileStmt.append(startLbl + "\n")
                .append("CJUMP " + n.f2.accept(this, argu) + " " + endLbl + "\n")
                .append(n.f4.accept(this, argu))
                .append("\nJUMP " + startLbl + "\n")
                .append(endLbl + " NOOP\n");
        return whileStmt.toString();
    }

    //**********************************************//
    // Visit expressions
    //**********************************************//
    @Override
    public String visit(Expression n, String argu) {
        return n.f0.accept(this, argu);
    }

    @Override
    public String visit(PlusExpression n, String argu) {
        String expr1 = n.f0.accept(this, argu);
        String expr2 = n.f2.accept(this, argu);
        return "PLUS " + expr1 + " " + expr2;
    }

    @Override
    public String visit(MinusExpression n, String argu) {
        String expr1 = n.f0.accept(this, argu);
        String expr2 = n.f2.accept(this, argu);
        return "MINUS " + expr1 + " " + expr2;
    }

    @Override
    public String visit(TimesExpression n, String argu) {
        String expr1 = n.f0.accept(this, argu);
        String expr2 = n.f2.accept(this, argu);
        return "TIMES " + expr1 + " " + expr2;
    }

    @Override
    public String visit(AndExpression n, String argu) {
        String endLabel = getLabel("AndEnd");
        String resultReg = getTemp();
        String clause1Code = n.f0.accept(this, argu);
        String clause2Code = n.f2.accept(this, argu);
        StringBuilder andExpr = new StringBuilder();
        andExpr.append(" " + stmtExpStart)
                .append("MOVE " + resultReg + clause1Code + "\n")
                .append("CJUMP " + resultReg + " " + endLabel + "\n")
                .append("MOVE " + resultReg + " " + clause2Code + "\n")
                .append(endLabel + " NOOP " + "\n")
                .append(stmtExpMiddle + resultReg + "\n")
                .append(stmtExpEnd);


        return andExpr.toString();
    }

    @Override
    public String visit(CompareExpression n, String argu) {
        String primExp1 = n.f0.accept(this, argu);
        String primExp2 = n.f2.accept(this, argu);
        return "LT " + primExp1 + " " + primExp2;
    }

    @Override
    public String visit(MessageSend n, String argu) {
        StringBuilder messageSend = new StringBuilder();
        String identifier = n.f0.accept(this, "ID");
        boolean checkNullity = false;
        // Check if identifier is a class field
        if (currentClass.containsField(identifier))
            checkNullity = true;
        String objTemp = getTemp();
        messageSend.append("CALL\n" + stmtExpStart
                + "MOVE " + objTemp + " ");
        String primExp = n.f0.accept(this, "");
        messageSend.append(primExp + " ");
        if (checkNullity)
            messageSend.append(checkingNullityCode(objTemp, "MessageSend"));
        // find the offset of the function to call
        String className = n.f0.accept(this, "className");
        if (argu.equals("className"))
            return className;
        ClassContainer classContainer = symbolTable.getClass(className);
        String functionName = n.f2.accept(this, "ID");
        FunctionContainer function = classContainer.getFunction(functionName);
        int offset = function.getOrder() - 1;
        // load VT into a temp
        String vtTemp = getTemp();
        messageSend.append("HLOAD " + vtTemp + " " + objTemp + " 0" + "\n");
        // load the functionLabel into another temp and return it
        String functionLabel = getTemp();
        messageSend.append("HLOAD " + functionLabel + " " + vtTemp + " " + (offset*4) + "\n")
                .append(stmtExpMiddle + " " + functionLabel + "\n");
        // prepare arguments, `this` and others
        messageSend.append(stmtExpEnd + " ( " + objTemp + " ");
        String arguments = "";
        if (n.f4.present()) {
            arguments = n.f4.accept(this, argu);
        }
        messageSend.append(arguments + " )\n");
        return messageSend.toString();
    }

    @Override
    public String visit(ExpressionList n, String argu) {
        String expr = n.f0.accept(this, argu);
        String others = n.f1.accept(this, argu);
        return expr + " " + others;
    }

    @Override
    public String visit(ExpressionTerm n, String argu) {
        return n.f1.accept(this, "");
    }

    @Override
    public String visit(ExpressionTail n, String argu) {
        StringBuilder otherExpr = new StringBuilder();
        if (n.f0.present()) {
            Enumeration<Node> exprTerms = n.f0.elements();
            while (exprTerms.hasMoreElements()) {
                ExpressionTerm exprTerm = (ExpressionTerm) exprTerms.nextElement();
                otherExpr.append(" " + exprTerm.f1.accept(this, argu));
            }
        }
        return otherExpr.toString();
    }

    @Override
    public String visit(PrimaryExpression n, String argu) {
        return n.f0.accept(this, argu);
    }

    @Override
    public String visit(NotExpression n, String argu) {
        String falseLbl = getLabel("false");
        String endLbl = getLabel("end");
        String returnReg = getTemp();
        String clauseCode = n.f1.accept(this, argu);
        StringBuilder notExpr = new StringBuilder();
        notExpr.append("\n" + stmtExpStart)
                .append("CJUMP " + clauseCode + " " + falseLbl + "\n")
                .append("MOVE " + returnReg + " 0\n")
                .append("JUMP " + endLbl + "\n")
                .append(falseLbl + " MOVE " + returnReg + " 1\n")
                .append(endLbl + " NOOP\n")
                .append(stmtExpMiddle + returnReg + "\n")
                .append(stmtExpEnd);
        return notExpr.toString();
    }

    @Override
    public String visit(AllocationExpression n, String argu) {
        String className = n.f1.accept(this, "ID");
        if (argu.equals("className"))
            return className;
        StringBuilder allocation = new StringBuilder();
        StringBuilder result = new StringBuilder();
        String objectTemp = getTemp();
        String vTableTemp = getTemp();
        ClassContainer classContainer = symbolTable.getClass(className);
        // allocation phase
        allocation.append("MOVE " + vTableTemp + " HALLOCATE "
                + (classContainer.getFunctions().size() * 4) + "\n");
        allocation.append("MOVE " + objectTemp + " HALLOCATE "
                + ((classContainer.getFields().size() + 1) * 4) + "\n");
        // assign functions to VT
        for (int i = 0; i < classContainer.getFunctions().size(); i++) {
            allocation.append("HSTORE " + vTableTemp + " " + (i * 4) + " "
                    + classContainer.getFunctionName(i + 1) + "\n");
        }
        // init fields
        // init all fields, with loop unrolling
        for (int i = 0; i < classContainer.getFields().size(); i++) {
            allocation.append("HSTORE PLUS " + objectTemp + " " + ((i + 1) * 4) + " 0 0\n");
        }
        // assign VT at object[0]
        allocation.append("HSTORE " + objectTemp + " 0 " + vTableTemp + "\n");
        result.append(stmtExpStart).append(allocation.toString())
                .append(stmtExpMiddle + " " + objectTemp + "\n").append(stmtExpEnd);
        return result.toString();
    }

    @Override
    public String visit(ArrayAllocationExpression n, String argu) {
        StringBuilder allocExpr = new StringBuilder();
        String baseAddr = getTemp();
        String idx = getTemp();
        String skipError = getLabel("skipError");
        String loopStart = getLabel("loopStart");
        String loopEnd = getLabel("loopEnd");
        // Allocate and set size (actual length)
        // Init all fields to 0, within a loop
        // Return the temp, holding the baseAddress
        String length = n.f3.accept(this, argu);
        allocExpr.append("\n" + stmtExpStart)
                .append("CJUMP LT " + length + " 1 " + skipError + "\n")
                .append("ERROR\n")
                .append(skipError + " MOVE " + baseAddr + " HALLOCATE TIMES 4 PLUS 1 " + length + "\n")
                .append("MOVE " + idx + " 4\n")
                .append(loopStart + " CJUMP LT " + idx + " TIMES 4 PLUS 1 " + length + " " + loopEnd + "\n")
                .append("HSTORE PLUS " + baseAddr + " " + idx + " 0 0\n")
                .append("MOVE " + idx + " PLUS " + idx + " 4\n")
                .append("JUMP " + loopStart + "\n")
                .append(loopEnd + " HSTORE " + baseAddr + " 0 " + length + "\n")
                .append(stmtExpMiddle + " " + baseAddr + "\n" + stmtExpEnd);
        return allocExpr.toString();
    }

    @Override
    public String visit(ArrayLookup n, String argu) {
        StringBuilder lookup = new StringBuilder();
        String identifier = n.f0.accept(this, "ID");
        // Temps
        String baseAddrTmp;
        String lengthTmp = getTemp();
        String valueTmp = getTemp();
        // Labels
        String upperOutOfBoundsLbl = getLabel("upperOutOfBounds");
        String correctLbl = getLabel("correct");
        // Different handling if identifier is a class field or a variable
        lookup.append("\n" + stmtExpStart);
        if (variableToRegister.containsKey(identifier)) {
            String baseAddrCode = n.f0.accept(this, argu);
            baseAddrTmp = getTemp();
            lookup.append("MOVE " + baseAddrTmp + " " + baseAddrCode + "\n");
        }
        else {
            // Class field int[]
            baseAddrTmp = getTemp();
            String skipNullLbl = getLabel("skipNull");
            // load base address from memory of the object
            lookup.append("HLOAD " + baseAddrTmp + " PLUS TEMP 0 " + (currentClass.getFieldOrder(identifier) * 4) + " 0\n");
            // check baseAddr
            lookup.append("CJUMP LT " + baseAddrTmp + " 1 " + skipNullLbl + "\n")
                    .append("ERROR\n")
                    .append(skipNullLbl + " NOOP\n");
        }
        // load length from heap
        lookup.append("HLOAD " + lengthTmp + " " + baseAddrTmp + " 0\n");
        String idx = n.f2.accept(this, argu);
        // check idx validity
        lookup.append("CJUMP LT "+ idx + " 0 " + upperOutOfBoundsLbl + "\n")
                .append("ERROR\n")
                .append(upperOutOfBoundsLbl + " CJUMP LT MINUS " + lengthTmp + " 1 " + idx + " " + correctLbl + "\n")
                .append("ERROR\n")
                .append(correctLbl + " NOOP\n")
                        // load value from heap position: baseAddr + (idx+1)*4
                .append("HLOAD " + valueTmp + " PLUS " + baseAddrTmp + " TIMES 4 PLUS 1 " + idx + " 0\n")
                .append(stmtExpMiddle + valueTmp + "\n")
                .append(stmtExpEnd);
        return lookup.toString();
    }

    @Override
    public String visit(ArrayLength n, String argu) {
        String baseAddr = n.f0.accept(this, "rvalue");
        String lengthTmp = getTemp();
        StringBuilder lengthExpr = new StringBuilder();
        lengthExpr.append("\n" + stmtExpStart)
                .append(checkingNullityCode(baseAddr, "ArrayLen"))
                .append("HLOAD " + lengthTmp + " " + baseAddr + " 0\n")
                .append(stmtExpMiddle + lengthTmp + "\n" + stmtExpEnd);
        return lengthExpr.toString();
    }

    public String checkingNullityCode(String baseAddr, String location) {
        String continueLabel = getLabel("continue" + location);
        StringBuilder checkNullCode = new StringBuilder();
        checkNullCode.append(" CJUMP LT " + baseAddr + " 1 " + continueLabel + "\n")
                .append("ERROR\n" + continueLabel + " NOOP\n");
        return checkNullCode.toString();
    }

    @Override
    public String visit(IntegerLiteral n, String argu) {
        return  " " + n.f0.withSpecials() + " ";
    }

    @Override
    public String visit(TrueLiteral n, String argu) {
        return " 1 ";
    }

    @Override
    public String visit(FalseLiteral n, String argu) {
        return " 0 ";
    }

    @Override
    public String visit(ThisExpression n, String argu) {
        if (argu.equals("className"))
            return currentClass.getName();
        return "TEMP 0";
    }

    @Override
    public String visit(BracketExpression n, String argu) {
        return n.f1.accept(this, argu);
    }

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
        String identifier = n.f0.withSpecials();
        if (argu.equals("ID"))
            return identifier;
        else if (argu.equals("className")) {
            // Find out the type of the identifier and return it
            if (currentFunction.getParams().containsKey(identifier))
                return currentFunction.getParams().get(identifier).getType();
            else if (currentFunction.getVars().containsKey(identifier))
                return currentFunction.getVars().get(identifier).getType();
            else {
                return currentClass.getFieldType(identifier);
            }
        }
        String virtualReg;
        if (!variableToRegister.containsKey(identifier)) {
            if (argu.equals("param"))
                virtualReg = getParam();
            else if (argu.equals("rvalue")) {
                // Case in which we reach here, in need of an rvalue
                // and the identifier is not a local variable or formal
                // parameter of the function
                String memReg = getTemp();
                StringBuilder hLoadStmtExpr = new StringBuilder();
                hLoadStmtExpr.append("\n" + stmtExpStart)
                        .append("HLOAD " + memReg + " PLUS TEMP 0 " + (currentClass.getFieldOrder(identifier)*4) + " 0\n")
                        .append(stmtExpMiddle + memReg + "\n" + stmtExpEnd);
                return hLoadStmtExpr.toString();
            }
            else if (currentClass.containsField(identifier)) {
                // case in which the identifier is a class field
                String memReg = getTemp();
                StringBuilder hLoadStmtExpr = new StringBuilder();
                hLoadStmtExpr.append("\n" + stmtExpStart)
                        .append("HLOAD " + memReg + " PLUS TEMP 0 " + (currentClass.getFieldOrder(identifier)*4) + " 0\n")
                        .append(stmtExpMiddle + memReg + "\n" + stmtExpEnd);
                return hLoadStmtExpr.toString();
            }
            else
                virtualReg = getTemp();
            variableToRegister.put(identifier, virtualReg);
            return virtualReg;
        }
        else {
            return variableToRegister.get(identifier);
        }
    }
    //**********************************************//


    //**********************************************//
    // ********** Generic functions ****************//
    //**********************************************//
    private String getLabel(String label) {
        if (currentFunction != null) {
            labelNo++;
            return currentFunction.getName() + "_" + label + (labelNo-1);
        }
        else
            return label;
    }

    private String getTemp() {
        tempNo++;
        return "TEMP " + (tempNo - 1);
    }

    private String getParam() {
        paramNo++;
        return "TEMP " + (paramNo - 1);
    }

    public void printProgram() {
        System.out.println("Printing program: " +
                "\n=============================");
        int lineNum = 0;
        String[] lines = program.toString().split("\n");
        for(String line: lines) {
            System.out.println(lineNum + "\t" + line);
            lineNum++;
        }
        System.out.println("=============================");
    }
}
