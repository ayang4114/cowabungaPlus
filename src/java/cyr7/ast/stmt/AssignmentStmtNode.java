package cyr7.ast.stmt;

import cyr7.ast.AbstractNode;
import cyr7.ast.AccessNode;
import cyr7.ast.expr.ExprNode;
import cyr7.exceptions.SemanticException;
import cyr7.exceptions.UnbalancedPushPopException;
import cyr7.semantics.Context;
import cyr7.semantics.ExpandedType;
import cyr7.semantics.OrdinaryType;
import cyr7.semantics.ResultType;
import cyr7.visitor.AbstractVisitor;
import java_cup.runtime.ComplexSymbolFactory;

public final class AssignmentStmtNode extends AbstractNode implements StmtNode {

    // Assign [value] to [node]
    public final AccessNode node;
    public final ExprNode value;

    public AssignmentStmtNode(ComplexSymbolFactory.Location location,
            AccessNode node, ExprNode value) {
        super(location);

        assert node != null;
        assert value != null;

        this.node = node;
        this.value = value;
    }

    @Override
    public <T> T accept(AbstractVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public boolean equals(Object o) {
        if (o instanceof AssignmentStmtNode) {
            AssignmentStmtNode oNode = (AssignmentStmtNode) o;
            return this.node.equals(oNode.node)
                    && this.value.equals(oNode.value);
        }
        return false;
    }

//    @Override
//    public ResultType typeCheck(Context c)
//            throws SemanticException, UnbalancedPushPopException {
//        OrdinaryType lhsType = node.typeCheck(c);
//        ExpandedType rhsType = value.typeCheck(c);
//        if (TypeCheckUtil.checkTypeEquality(lhsType, rhsType)) {
//            return ResultType.UNIT;
//        } else {
//            throw new SemanticException("Inequivalent type assignment");
//        }
//    }

}
