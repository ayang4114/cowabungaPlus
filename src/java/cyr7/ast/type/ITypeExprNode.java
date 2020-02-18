package cyr7.ast.type;

import cyr7.ast.AbstractNode;
import cyr7.ast.expr.ExprNode;
import cyr7.exceptions.SemanticException;
import cyr7.semantics.Context;
import cyr7.semantics.ExpandedType;
import cyr7.semantics.OrdinaryType;
import java_cup.runtime.ComplexSymbolFactory.Location;

import java.util.Collections;
import java.util.List;
import java.util.Optional;


/**
 * Represents an explicit variable type
 */
public abstract class ITypeExprNode extends AbstractNode {


    public ITypeExprNode(Location location) {
        super(location);
    }
    
    /**
	 * @param - primitive type of the larger type
	 * @param - dimensionList - the list of array dimensions associated with the type, with Optional.empty()
	 * representing no size was given for that dimension. dimensionList must be passed in order, i.e. to create
	 * the type for int[4][3][], we pass in: {Optional.of(4), Optional.of(3), Optional.empty()}
	 * @return [node] - an ITypeExprNode representing a recursive definition of the type of the object
	 */
    static ITypeExprNode fromDimensionList(PrimitiveTypeNode primitive, 
            List<Optional<ExprNode>> dimensionList) {
        ITypeExprNode node = primitive;
        Collections.reverse(dimensionList);
        for (Optional<ExprNode> e : dimensionList) {
            node = new TypeExprArrayNode(
                    primitive.getLocation().orElse(null), node, e);
        }
        return node;
    }
    
    public abstract OrdinaryType typeCheck(Context c) throws SemanticException;


}
