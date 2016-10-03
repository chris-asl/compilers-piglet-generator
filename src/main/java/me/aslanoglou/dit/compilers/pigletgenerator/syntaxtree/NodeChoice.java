//
// Generated by JTB 1.3.2 DIT@UoA patched
//

package me.aslanoglou.dit.compilers.pigletgenerator.syntaxtree;
import me.aslanoglou.dit.compilers.pigletgenerator.visitor.GJNoArguVisitor;
import me.aslanoglou.dit.compilers.pigletgenerator.visitor.GJVisitor;
import me.aslanoglou.dit.compilers.pigletgenerator.visitor.GJVoidVisitor;
import me.aslanoglou.dit.compilers.pigletgenerator.visitor.Visitor;

/**
 * Represents a grammar choice, e.g. ( A | B )
 */
public class NodeChoice implements Node {
   public NodeChoice(Node node) {
      this(node, -1);
   }

   public NodeChoice(Node node, int whichChoice) {
      choice = node;
      which = whichChoice;
   }

   public void accept(Visitor v) {
      choice.accept(v);
   }
   public <R,A> R accept(GJVisitor<R,A> v, A argu) {
      return choice.accept(v,argu);
   }
   public <R> R accept(GJNoArguVisitor<R> v) {
      return choice.accept(v);
   }
   public <A> void accept(GJVoidVisitor<A> v, A argu) {
      choice.accept(v,argu);
   }

   public Node choice;
   public int which;
}

