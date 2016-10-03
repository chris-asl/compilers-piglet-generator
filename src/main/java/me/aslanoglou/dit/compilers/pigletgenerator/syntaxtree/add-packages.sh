#!/bin/bash

ast_files=(*);
for f in ${ast_files[@]}
do
        gsed -i '/package/ a\import me.aslanoglou.dit.compilers.pigletgenerator.visitor.GJNoArguVisitor;\nimport me.aslanoglou.dit.compilers.pigletgenerator.visitor.GJVisitor;\nimport me.aslanoglou.dit.compilers.pigletgenerator.visitor.GJVoidVisitor;\nimport me.aslanoglou.dit.compilers.pigletgenerator.visitor.Visitor;' $f
import me.aslanoglou.dit.compilers.pigletgenerator.visitor.GJNoArguVisitor;
import me.aslanoglou.dit.compilers.pigletgenerator.visitor.GJVisitor;
import me.aslanoglou.dit.compilers.pigletgenerator.visitor.GJVoidVisitor;
import me.aslanoglou.dit.compilers.pigletgenerator.visitor.Visitor;
done
echo "Done!"
exit 0;
