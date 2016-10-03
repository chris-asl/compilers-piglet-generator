//
// Generated by JTB 1.3.2 DIT@UoA patched
//

package me.aslanoglou.dit.compilers.pigletgenerator.syntaxtree;
import me.aslanoglou.dit.compilers.pigletgenerator.visitor.GJNoArguVisitor;
import me.aslanoglou.dit.compilers.pigletgenerator.visitor.GJVisitor;
import me.aslanoglou.dit.compilers.pigletgenerator.visitor.GJVoidVisitor;
import me.aslanoglou.dit.compilers.pigletgenerator.visitor.Visitor;

/**
 * Grammar production:
 * f0 -> Identifier()
 * f1 -> "["
 * f2 -> Expression()
 * f3 -> "]"
 * f4 -> "="
 * f5 -> Expression()
 * f6 -> ";"
 */
public class ArrayAssignmentStatement implements Node {
   public Identifier f0;
   public NodeToken f1;
   public Expression f2;
   public NodeToken f3;
   public NodeToken f4;
   public Expression f5;
   public NodeToken f6;

   public ArrayAssignmentStatement(Identifier n0, NodeToken n1, Expression n2, NodeToken n3, NodeToken n4, Expression n5, NodeToken n6) {
      f0 = n0;
      f1 = n1;
      f2 = n2;
      f3 = n3;
      f4 = n4;
      f5 = n5;
      f6 = n6;
   }

   public ArrayAssignmentStatement(Identifier n0, Expression n1, Expression n2) {
      f0 = n0;
      f1 = new NodeToken("[");
      f2 = n1;
      f3 = new NodeToken("]");
      f4 = new NodeToken("=");
      f5 = n2;
      f6 = new NodeToken(";");
   }

   public void accept(Visitor v) {
      v.visit(this);
   }
   public <R,A> R accept(GJVisitor<R,A> v, A argu) {
      return v.visit(this,argu);
   }
   public <R> R accept(GJNoArguVisitor<R> v) {
      return v.visit(this);
   }
   public <A> void accept(GJVoidVisitor<A> v, A argu) {
      v.visit(this,argu);
   }
}

