package cyr7.cfg.nodes;

import java.util.List;

import cyr7.cfg.visitor.AbstractCFGVisitor;
import cyr7.ir.cfg.StubCFGNode;
import cyr7.ir.nodes.IRExpr;
import java_cup.runtime.ComplexSymbolFactory.Location;

public class CFGMemAssignNode extends CFGNode {

    public final IRExpr target;
    public final IRExpr value;
    private CFGNode out;

    public CFGMemAssignNode(Location location, IRExpr target, IRExpr value,
            CFGNode out) {
        super(location);
        this.target = target;
        this.value = value;
        this.out = out;

        this.updateIns();
    }

    @Override
    public List<CFGNode> out() {
        return List.of(this.out);
    }

    @Override
    public <T> T accept(AbstractCFGVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void convertFromStub(StubCFGNode stub, CFGNode n) {
        if (out == stub) {
            this.out = n;
            this.updateIns();
        } else {
            throw new UnsupportedOperationException(
                    "Cannot change out node unless it was originally a stub node.");
        }
    }

}