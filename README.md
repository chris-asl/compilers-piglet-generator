    **********************************************************************
    ** Compilers - Spring Semester 2014                                 **
    ** Semester Project: MiniJava (subset of Java) compiler             **
    ** Part 2 out of 4: Piglet Generator                                **
    **********************************************************************
### Piglet Generator for miniJava, a subset of Java
_Project assignment with more information on MiniJava can be found [here](http://cgi.di.uoa.gr/~thp06/13_14/project.html#Homework_3_-_Intermediate_Code_)._

This is the second out of four parts of the semester project for the Compilers course.  
Input is comprised of miniJava source files that will be compiled to Piglet.

We were provided with Piglet grammar in JavaCC form and also, two java programs, a Piglet interpreter `pgi.jar` and a piglet code pretty-printer `pretty-printer.jar`.

##### A. Build & package to jar
Run `mvn package`.  
File `minijava-minijava-semantical-analyzer-VERSION.jar` will be created under `target` directory.  

##### B. Run
The semantical analyzer expects (any number) miniJava source files as input.  
These will be compiled to Piglet code (`*.pg` files).  

###### B.1 Usage example   
`java [MainClassName] [file1] [file2] ... [fileN]`

###### B.2 How to run  
`java -jar /path/to/jar/file/minijava-semantical-analyzer-1.0.jar [javaSrcFile1] .. [javaSrcFileN]` 

###### B.3 Tests  
We were provided with some test files (located under `src/test/resources/minijava-test-files`) and their equivalent piglet files.  
To validate the generated Piglet code, it suffices to compile the miniJava test files to bytecode and to Piglet, run them, save their output and compare to find any difference.  
_This is done by `tests.sh` and `assert_equal_output.sh` scripts._

To run all tests (from both directories), simply run `tests.sh`.