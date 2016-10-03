package me.aslanoglou.dit.compilers.pigletgenerator;

import me.aslanoglou.dit.compilers.pigletgenerator.parser.MiniJavaParser;
import me.aslanoglou.dit.compilers.pigletgenerator.parser.ParseException;
import me.aslanoglou.dit.compilers.pigletgenerator.syntaxtree.Goal;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * MiniJava compiler to Piglet.
 * Compilers - 3rd Assignmnent
 * Author: Chris Aslanoglou
 */
class Driver {
    public static void main (String [] args){
        if(args.length == 0){
            System.err.println("Usage: java Driver [<inputFile>]+");
            System.exit(1);
        }
        for (int i = 0; i < args.length; i++) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(args[i]);
                System.out.println("================================================");
                System.out.println("\t[1/2] Gathering source file information for: '" + args[i] + "'");
                MiniJavaParser parser = new MiniJavaParser(fis);
                // Pass 1 - ClassNames collection
                ClassNameCollector classNameCollector = new ClassNameCollector();
                Goal tree = parser.Goal();
                tree.accept(classNameCollector, null);
                // Pass 2 - ClassMembers collection
                ClassMembersVisitor classMembersVisitor =
                        new ClassMembersVisitor(classNameCollector.verifiedClassNames,
                                classNameCollector.superClassRelation);
                tree.accept(classMembersVisitor, "Phase2");
                // Piglet generator
                String programName = prepareFilename(args[i]);
                System.out.println("\t[2/2] Generating piglet source file: " + programName);
                PrintWriter writer = null;
                try {
                    writer = new PrintWriter(programName, "UTF-8");
                    PigletGenerator pigletGenerator = new PigletGenerator(classMembersVisitor.verifiedClassNames,
                            classMembersVisitor.superClassRelation, classMembersVisitor.classInfo);
                    tree.accept(pigletGenerator, "PigletGenerator");
                    writer.print(pigletGenerator.program.toString());
                    System.out.println("\tPiglet source code generated.");
                    System.out.println("================================================");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } finally {
                    if (writer != null)
                        writer.close();
                }
            } catch (ParseException ex) {
                System.out.println(ex.getMessage());
            } catch (FileNotFoundException ex) {
                System.err.println(ex.getMessage());
            } finally {
                try {
                    if (fis != null) fis.close();
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
    }

    private static String prepareFilename(String absPath) {
        Path p = Paths.get(absPath);
        String programName = p.getFileName().toString();
        int idxOfDot = programName.lastIndexOf('.');
        programName = programName.substring(0, idxOfDot);
        return p.getParent() + "/" + programName + ".pg";
    }
}
