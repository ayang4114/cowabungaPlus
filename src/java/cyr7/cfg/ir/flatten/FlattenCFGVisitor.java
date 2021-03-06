package cyr7.cfg.ir.flatten;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

import cyr7.cfg.ir.nodes.CFGBlockNode;
import cyr7.cfg.ir.nodes.CFGCallNode;
import cyr7.cfg.ir.nodes.CFGIfNode;
import cyr7.cfg.ir.nodes.CFGMemAssignNode;
import cyr7.cfg.ir.nodes.CFGNode;
import cyr7.cfg.ir.nodes.CFGReturnNode;
import cyr7.cfg.ir.nodes.CFGSelfLoopNode;
import cyr7.cfg.ir.nodes.CFGStartNode;
import cyr7.cfg.ir.nodes.CFGStubNode;
import cyr7.cfg.ir.nodes.CFGVarAssignNode;
import cyr7.cfg.ir.visitor.IrCFGVisitor;
import cyr7.ir.DefaultIdGenerator;
import cyr7.ir.IdGenerator;
import cyr7.ir.nodes.IRJump;
import cyr7.ir.nodes.IRLabel;
import cyr7.ir.nodes.IRNodeFactory;
import cyr7.ir.nodes.IRNodeFactory_c;
import cyr7.ir.nodes.IRStmt;
import java_cup.runtime.ComplexSymbolFactory.Location;
import polyglot.util.Pair;

public class FlattenCFGVisitor
        implements IrCFGVisitor<Optional<CFGNode>> {

    /**
     * A wrapper class so that stmts can be compared via pointer addresses,
     * instead of their overwritten equals() methods.
     */
    private class IRStmtWrapper {

        private Optional<IRLabel> label;
        private Optional<IRJump> jump;

        protected final List<IRStmt> stmt;

        public IRStmtWrapper(List<IRStmt> stmts) {
            this.stmt = Collections.unmodifiableList(stmts);
            this.label = Optional.empty();
            this.jump = Optional.empty();
        }

        public IRStmtWrapper(IRStmt stmt) {
            this.stmt = List.of(stmt);
            this.label = Optional.empty();
            this.jump = Optional.empty();
        }

        public IRStmtWrapper() {
            this.stmt = List.of();
            this.label = Optional.empty();
            this.jump = Optional.empty();
        }

        public Location location() {
            if (stmt.isEmpty())
                return new Location(-1, -1);
            else
                return stmt.get(0).location();
        }

        public void setLabel(String label) {
            if (this.label.isPresent()) {
                throw new UnsupportedOperationException(
                        "Error: Cannot set label twice.");
            }
            var make = new IRNodeFactory_c(this.location());
            this.label = Optional.of(make.IRLabel(label));
        }

        public void setJump(String target) {
            if (this.jump.isPresent()) {
                throw new UnsupportedOperationException(
                        "Error: Cannot set jump twice.");
            }
            var make = new IRNodeFactory_c(this.location());
            this.jump = Optional.of(make.IRJump(make.IRName(target)));
        }
    }

    /**
     * This is the list of statements corresponding to this control flow graph.
     */
    private final List<IRStmtWrapper> stmts;

    /**
     * This is the set of nodes already visited by the visitor class. Each visit
     * method should add the current node to this set.
     */
    private final HashSet<CFGNode> visitedNodes;

    /**
     * Mapping from a CFGNode to a IRLabel, if such a label has been created at
     * this particular program point. When an IRLabel is inserted into the list
     * of statements {@code stmts}, the pair of CFGNode and that label name
     * should be added to this map.
     */
    private final IdentityHashMap<CFGNode, String> cfgNodeToLabels;

    /**
     * Mapping from a CFGNode to an IRStmt. If a CFGNode generates an IRNode in
     * a visit method, then the pairing should be added to this mapping.
     */
    private final IdentityHashMap<CFGNode, IRStmtWrapper> cfgNodeToIRStmt;

    /**
     * Queue of untransformed true branches, created from CFGIfNodes. When
     * visiting a CFGIfNode, add the true branch to this queue, and continue
     * propogating/transforming via the false branch.
     * <p>
     * The CFGNode sub-graphs in this collection are then transformed via the
     * same strategy. The particular collection implementation used here does
     * not matter; the order in which these CFGNode sub-graphs are transformed
     * is not important.
     */
    private final Queue<Pair<CFGNode, String>> trueBranches;

    /**
     * The previous IRStmt added to the list of statements when traversing
     * through CFG sub-graph.
     */
    private IRStmtWrapper predecessor;

    private final IdGenerator generator;

    protected FlattenCFGVisitor() {
        this.visitedNodes = new HashSet<>();
        this.cfgNodeToLabels = new IdentityHashMap<>();
        this.cfgNodeToIRStmt = new IdentityHashMap<>();
        this.trueBranches = new ArrayDeque<>();
        this.stmts = new ArrayList<>();
        this.generator = new DefaultIdGenerator();
    }


    protected FlattenCFGVisitor(IdGenerator generator) {
        this.visitedNodes = new HashSet<>();
        this.cfgNodeToLabels = new IdentityHashMap<>();
        this.cfgNodeToIRStmt = new IdentityHashMap<>();
        this.trueBranches = new ArrayDeque<>();
        this.stmts = new ArrayList<>();
        this.generator = generator;
    }



    /**
     * Sets node {@code n} to be visited and the predecessor node for the next
     * node.
     *
     * @param n
     */
    private void epilogueProcess(CFGNode n, IRStmtWrapper stmt) {
        this.visitedNodes.add(n);
        this.predecessor = stmt;
    }

    private IRNodeFactory createMake(CFGNode n) {
        return new IRNodeFactory_c(n.location());
    }

    private IRStmtWrapper wrapStmt(IRStmt s) {
        return new IRStmtWrapper(s);
    }

    private IRStmtWrapper wrapStmt(List<IRStmt> s) {
        return new IRStmtWrapper(s);
    }

    private IRStmtWrapper wrapStmt() {
        return new IRStmtWrapper();
    }

    private void insertJumpForNode(IRStmtWrapper from, String target) {
        from.setJump(target);
    }

    /**
     * Inserts a newly-created label before the IRStmt associated to node
     * {@code n}.
     *
     * @param n
     */
    private String insertLabelForNode(CFGNode n) {
        final var stmt = this.cfgNodeToIRStmt.get(n);
        final String label = generator.newLabel();
        stmt.setLabel(label);
        this.cfgNodeToLabels.put(n, label);
        return label;
    }

    protected List<IRStmt> getFunctionBody() {
        return this.stmts.stream().flatMap(wrapper -> {
            final List<IRStmt> content = new ArrayList<>();
            wrapper.label.ifPresent(lbl -> content.add(lbl));
            wrapper.stmt.forEach(s -> content.add(s));
            wrapper.jump.ifPresent(jump -> content.add(jump));
            return content.stream();
        }).collect(Collectors.toList());
    }

    private boolean performProcessIfVisited(CFGNode n) {
        if (this.visitedNodes.contains(n)) {
            if (this.cfgNodeToLabels.containsKey(n)) {
                final String target = this.cfgNodeToLabels.get(n);
                this.insertJumpForNode(this.predecessor, target);
            } else {
                String target = this.insertLabelForNode(n);
                this.insertJumpForNode(this.predecessor, target);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Adds the statement {@code stmt} to the end of the list of statements.
     * <p>
     * Associates node {@code n} to this statement {@code stmt} in
     * {@code cfgNodeToIRStmt}.
     *
     * @param n
     * @param stmt
     */
    private void appendStmt(CFGNode n, IRStmtWrapper stmt) {
        final var wrappedStmt = stmt;
        this.stmts.add(wrappedStmt);
        this.wrapStmt().label.ifPresent(lbl ->
                this.cfgNodeToLabels.put(n, lbl.name()));
        this.cfgNodeToIRStmt.put(n, wrappedStmt);
    }

    @Override
    public Optional<CFGNode> visit(CFGCallNode n) {
        if (!this.performProcessIfVisited(n)) {
            final var stmt = this.wrapStmt(n.call);
            this.appendStmt(n, stmt);
            this.epilogueProcess(n, stmt);
            final CFGNode next = n.out().get(0);
            return Optional.of(next);
        }
        return Optional.empty();
    }

    @Override
    public Optional<CFGNode> visit(CFGIfNode n) {
        if (!this.performProcessIfVisited(n)) {
            final CFGNode trueBranch = n.trueBranch();
            final String trueLabel = generator.newLabel();
            this.trueBranches.add(new Pair<>(trueBranch, trueLabel));

            final var make = this.createMake(n);
            final CFGNode falseBranch = n.falseBranch();
            final var stmt = this.wrapStmt(make.IRCJump(n.cond, trueLabel));
            this.appendStmt(n, stmt);
            this.epilogueProcess(n, stmt);
            return Optional.of(falseBranch);
        }
        return Optional.empty();
    }

    @Override
    public Optional<CFGNode> visit(CFGVarAssignNode n) {
        if (!this.performProcessIfVisited(n)) {
            final var make = this.createMake(n);
            final var stmt = this
                    .wrapStmt(make.IRMove(make.IRTemp(n.variable), n.value));
            this.appendStmt(n, stmt);
            this.epilogueProcess(n, stmt);
            final CFGNode next = n.out().get(0);
            return Optional.of(next);
        }
        return Optional.empty();
    }

    @Override
    public Optional<CFGNode> visit(CFGMemAssignNode n) {
        if (!this.performProcessIfVisited(n)) {
            final var make = this.createMake(n);
            final var stmt = this.wrapStmt(make.IRMove(n.target, n.value));
            this.appendStmt(n, stmt);
            this.epilogueProcess(n, stmt);
            final CFGNode next = n.out().get(0);
            return Optional.of(next);
        }
        return Optional.empty();
    }

    /**
     * Three possibilities:
     * <ol>
     * <li>This node has never been visited.
     * <li>This node has been visited but there is no associated label to jump
     * to.
     * <li>This node has been visited and there is an associated label.
     * </ol>
     */
    @Override
    public Optional<CFGNode> visit(CFGReturnNode n) {
        if (!this.performProcessIfVisited(n)) {
            final var make = this.createMake(n);
            var stmt = this.wrapStmt(make.IRReturn());
            this.appendStmt(n, stmt);
            this.epilogueProcess(n, stmt);
        }
        return Optional.empty();
    }

    /**
     * There are no labels or statements associated with the start node.
     */
    @Override
    public Optional<CFGNode> visit(CFGStartNode n) {
        if (!this.performProcessIfVisited(n)) {
            var make = this.createMake(n);
            var startElement = this.wrapStmt();
            this.appendStmt(n, startElement);
            this.epilogueProcess(n, startElement);

            Optional<CFGNode> next = Optional.of(n.out().get(0));
            while (next.isPresent()) {
                next = next.get().accept(this);
            }

            while (!this.trueBranches.isEmpty()) {
                final Pair<CFGNode, String> nextTrueBranch = trueBranches
                        .poll();
                next = Optional.of(nextTrueBranch.part1());
                make = this.createMake(next.get());
                final String trueLabel = nextTrueBranch.part2();

                var stmt = this.wrapStmt(make.IRLabel(trueLabel));
                this.predecessor = stmt;
                this.stmts.add(stmt);
                while (next.isPresent()) {
                    next = next.get().accept(this);
                }
            }
            return Optional.empty();
        }
        return Optional.empty();
    }

    @Override
    public Optional<CFGNode> visit(CFGSelfLoopNode n) {
        if (!this.performProcessIfVisited(n)) {
            final var make = this.createMake(n);
            final var labelString = generator.newLabel();
            final var stmt = this.wrapStmt(make.IRJump(make.IRName(labelString)));
            stmt.setLabel(labelString);
            this.appendStmt(n, stmt);
            this.epilogueProcess(n, stmt);
        }
        return Optional.empty();
    }

    @Override
    public Optional<CFGNode> visit(CFGBlockNode n) {
        if (!this.performProcessIfVisited(n)) {
            List<IRStmt> stmts = new FlattenCFGBlockVisitor().getStmts(n.block);
            final var stmt = this.wrapStmt(stmts);
            this.appendStmt(n, stmt);
            this.epilogueProcess(n, stmt);
            final CFGNode next = n.out().get(0);
            return Optional.of(next);
        }
        return Optional.empty();
    }


    private class FlattenCFGBlockVisitor implements IrCFGVisitor<IRStmt> {

        private IRNodeFactory createMake(CFGNode n) {
            return new IRNodeFactory_c(n.location());
        }

        public List<IRStmt> getStmts(CFGNode topNode) {
            final var stmts = new ArrayList<IRStmt>();
            CFGNode blockToTraverse = topNode;
            while (!(blockToTraverse instanceof CFGStubNode)) {
                stmts.add(blockToTraverse.accept(this));
                blockToTraverse = blockToTraverse.out().get(0);
            }
            return stmts;
        }

        @Override
        public IRStmt visit(CFGCallNode n) {
            return n.call;
        }

        @Override
        public IRStmt visit(CFGIfNode n) {
            throw new AssertionError("Cannot have nested if node inside block");
        }

        @Override
        public IRStmt visit(CFGVarAssignNode n) {
            final var make = this.createMake(n);
            return make.IRMove(make.IRTemp(n.variable), n.value);
        }

        @Override
        public IRStmt visit(CFGMemAssignNode n) {
            final var make = this.createMake(n);
            return make.IRMove(n.target, n.value);
        }

        @Override
        public IRStmt visit(CFGReturnNode n) {
            throw new AssertionError("Cannot have return node inside block");
        }

        @Override
        public IRStmt visit(CFGStartNode n) {
            throw new AssertionError("Cannot have start node inside block");
        }

        @Override
        public IRStmt visit(CFGSelfLoopNode n) {
            throw new AssertionError("Cannot have self-loop node inside block");
        }

        @Override
        public IRStmt visit(CFGBlockNode n) {
            throw new AssertionError("Cannot have nested cfg block node");
        }

    }


}