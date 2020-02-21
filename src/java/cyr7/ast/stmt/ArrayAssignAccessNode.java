package cyr7.ast.stmt;

import cyr7.ast.expr.ExprNode;
import cyr7.exceptions.SemanticException;
import cyr7.semantics.ArrayType;
import cyr7.semantics.Context;
import cyr7.semantics.OrdinaryType;
import cyr7.semantics.PrimitiveType;
import cyr7.visitor.AbstractVisitor;
import java_cup.runtime.ComplexSymbolFactory.Location;

/**
 * Represents assigning an array index to a value. Ex: the LHS of arr[3][4] = 3 
 * would be represented by 
 * ArrayAssignAccessNode(
 *      ArrayAssignAccessNode(VariableAssignAccessNode("arr"), 3), 4)
 */
public final class ArrayAssignAccessNode extends AssignAccessNode {

    public final AssignAccessNode node;
    public final ExprNode index;
	
    public ArrayAssignAccessNode(Location location, AssignAccessNode node, 
            ExprNode index) {
        super(location);

        assert node != null;
        assert index != null;

        this.node = node;
        this.index = index;
    }
    
    @Override
    public <T> T accept(AbstractVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public boolean equals(Object o) {
        if (o instanceof ArrayAssignAccessNode) {
            ArrayAssignAccessNode oNode = (ArrayAssignAccessNode) o;
            return this.node.equals(oNode.node)
                    && this.index.equals(oNode.index)
                    && this.node.equals(oNode.node);
        }
        return false;
    }

    @Override
    public OrdinaryType typeCheck(Context c) throws SemanticException {
        OrdinaryType type = this.node.typeCheck(c);
        if (!TypeCheckUtil.checkTypeEquality(index.typeCheck(c),
                PrimitiveType.INT)) {
        	throw new SemanticException("Index into array must be an int");
        }
        return new ArrayType(type);
    }

}
