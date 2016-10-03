#!/usr/bin/env bash
####################################################################################################################
# Author: Chris Aslanoglou
# Description:
#   Validate same output by running both miniJava test files and generated Piglet files.
#   More on assert_equal_output script.
####################################################################################################################

RED='\033[0;31m'
GREEN='\033[0;32m'
BROWN_ORANGE='\033[0;33m'
NC='\033[0m' # No Color

allTestsDir="$(pwd)/src/test/resources/"
courseTests="${allTestsDir}minijava-test-files/"
assertEqualOutputScript=./assert_equal_output.sh

# Ensure test directories exist
function ensureDirExists {
    if [ $# -ne 1 ]; then
        echo -e "${RED}Missing directory argument.${NC}"
        exit -2
    fi
    dir=$1
    if [ ! -d ${dir} ]; then
        echo -e "${RED}${dir} doesn't exist. Either create it or update the variables in this script.${NC}"
        exit -1
    fi
}
ensureDirExists ${allTestsDir}
ensureDirExists ${courseTests}

if [ ! -f ${assertEqualOutputScript} ]; then
    echo -e "${RED}Expected: $assertEqualOutputScript.${NC}"
    exit -1;
fi

# Ensure jar exists
targetDir="$(pwd)/target"
jar="${targetDir}/minijava-piglet-generator-1.0.jar"

if [ ! -f ${jar} ]; then
    echo "${jar} is missing. Run \'mvn package\'."
    exit -1;
fi

# Testing
shopt -s nullglob  # Causes the array to be empty if none mach.
courseTestFiles=(${allTestsDir}minijava-*/*.java)
pgi="pgi.jar"

totalNumberOfTests=${#courseTestFiles[@]}
i=1
for file in "${courseTestFiles[@]}"
do
    # Generate Piglet code
    java -jar ${jar} ${file} >/dev/null 2>&1
    ${assertEqualOutputScript} ${file}

    printf "${GREEN}Completed${NC} $i/${totalNumberOfTests}: ${file}\n"
    ((i++))
done
printf "${GREEN}\rTests completed.${NC}\n"

exit 0;
