#!/bin/bash
####################################################################################################################
# Author: Chris Aslanoglou
# Description:
#   Expects a miniJava source file (absolute path) and
#       1. Compiles it to Java bytecode and executes it while saving its output
#       2. Runs it with piglet interpreter (pgi) while saving its output
#       If outputs differ, an error message is printed, otherwise success confirmation is printed.
####################################################################################################################

if [ $# -ne 1 ]; then
    echo "Usage: $0 <java-source-file>"
    exit -1;
fi

RED='\033[0;31m'
GREEN='\033[0;32m'
BROWN_ORANGE='\033[0;33m'
NC='\033[0m' # No Color

testFileDir=$(dirname $1)
program=$(basename $1)
program=${program%.java}
javaFile=$1
javaOut="$1.out"
pgFile="${1%java}pg"
pgOut="$pgFile.out"
pgi="pgi.jar"

function ensureFileExists {
    if [ ! -f $1 ]; then
        echo "${RED}$1: No such file${NC}"
        exit -1
    fi
}
function ensureSuccess {
    if [ $1 -ne 0 ]; then
        exit -1;
    fi
}
function printInfo {
    echo -e "${BROWN_ORANGE}$@${NC}"
}

ensureFileExists ${pgi}

# Compile and run Java code
# printInfo "Compiling Java: $javaFile"
javac ${javaFile}
ensureSuccess $?
# printInfo "Running Java program: $program"
java -classpath ${testFileDir} ${program} > ${javaOut}
ensureSuccess $?

# Compile and run Piglet code

# printInfo "Generating Piglet: $javaFile"
java -jar pgi.jar < ${pgFile} > ${pgOut}
ensureSuccess $?

# Ensure that two output files match
diff ${javaOut} ${pgOut}	2>/dev/null 1>&2

if [ $? -ne 0 ]; then
	echo -e "\t${RED}Output not identical for $javaFile!${NC}"
else
	echo -e "\t${GREEN}Identical output for $javaFile.${NC}"
fi

# Cleanup
# Treating .class files differently, since many are created per .java file (due to many classes in one .java file)
rm ${pgOut} ${pgFile} ${javaOut} -f
find ${testFileDir} -name "*.class" | xargs rm -f

exit 0;
