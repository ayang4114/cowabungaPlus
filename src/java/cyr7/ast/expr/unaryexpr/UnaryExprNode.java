package cyr7.ast.expr.unaryexpr;

import cyr7.ast.expr.ExprNode;
import java_cup.runtime.ComplexSymbolFactory;

/**
 * Superclass to expressions involving one child: -[ExprNode expr] or ![ExprNode expr]
 */
public abstract class UnaryExprNode extends ExprNode {
    final ExprNode expr;

    public UnaryExprNode(ComplexSymbolFactory.Location location, ExprNode expr) {
        super(location);
        this.expr = expr;
    }

}
