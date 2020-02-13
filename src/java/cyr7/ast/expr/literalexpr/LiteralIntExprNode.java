package cyr7.ast.expr.literalexpr;

import cyr7.ast.expr.ExprNode;
import edu.cornell.cs.cs4120.util.SExpPrinter;
import java_cup.runtime.ComplexSymbolFactory;

/**
 * Represents an integer literal, Ex: [contents] = "20"
 */
public class LiteralIntExprNode extends ExprNode {

    final String contents;

    public LiteralIntExprNode(ComplexSymbolFactory.Location location, String contents) {
        super(location);
        this.contents = contents;
    }

    @Override
    public void prettyPrint(SExpPrinter printer) {
        printer.printAtom(contents);
    }
    
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof LiteralIntExprNode) {
            LiteralIntExprNode oNode = (LiteralIntExprNode) o;
            return this.contents.equals(oNode.contents);
        }
        return false;    
    }
    
}
