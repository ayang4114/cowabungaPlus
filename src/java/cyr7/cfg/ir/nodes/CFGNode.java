package cyr7.cfg.ir.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cyr7.cfg.ir.dfa.BackwardTransferFunction;
import cyr7.cfg.ir.dfa.ForwardTransferFunction;
import cyr7.cfg.ir.visitor.IrCFGVisitor;
import java_cup.runtime.ComplexSymbolFactory.Location;

public abstract class CFGNode {

    protected final List<CFGNode> in;

    protected boolean repOk() {
        for (CFGNode n: this.out()) {
            if (!n.in().contains(this)) return false;
        }
        for (CFGNode n: this.in()) {
            if (!n.out().contains(this)) { return false;
            }
        }
        return true;
    }

    private final Location location;

    protected CFGNode(Location location) {
        this.in = new ArrayList<>(1);
        this.location = location;
    }

    public List<CFGNode> in() {
        return in;
    }

    public Location location() {
        return location;
    }

    public abstract List<CFGNode> out();

    public abstract <T> T accept(IrCFGVisitor<T> visitor);

    /**
     * Invariant: the number of elements in the output of this list must
     * correspond one-to-one with the elements of out().
     */
    public abstract <T> List<T> acceptForward(ForwardTransferFunction<T> transferFunction, T input);

    public abstract <T> T acceptBackward(BackwardTransferFunction<T> transferFunction, T input);

    protected final void updateIns() {
        for (CFGNode node : out()) {
            if (!node.in().contains(this)) {
                node.in().add(this);
            }
        }
    }

    public abstract void replaceOutEdge(CFGNode previous, CFGNode newTarget);

    public abstract CFGNode copy(List<CFGNode> out);


    public abstract Set<String> defs();
    public abstract Set<String> uses();

    public abstract Map<String, String> gens();
    public abstract Set<String> kills();

    public abstract void refreshDfaSets();

}