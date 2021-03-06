package cyr7.ast.stmt;

import cyr7.ast.AbstractNode;
import cyr7.ast.Node;
import cyr7.ast.type.TypeExprArrayNode;
import cyr7.visitor.AstVisitor;
import java_cup.runtime.ComplexSymbolFactory.Location;

import java.util.List;

/**
 * A statement of the form
 * a: int[e1]...[en][]...[]
 */
public final class ArrayDeclStmtNode extends AbstractNode implements StmtNode {

    public final String identifier;

    public final TypeExprArrayNode type;

    public ArrayDeclStmtNode(Location location, String identifier,
                             TypeExprArrayNode type) {
        super(location);
        this.identifier = identifier;
        this.type = type;
    }

    @Override
    public List<Node> getChildren() {
        return List.of(type);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayDeclStmtNode that = (ArrayDeclStmtNode) o;
        return identifier.equals(that.identifier) &&
            type.equals(that.type);
    }

}
