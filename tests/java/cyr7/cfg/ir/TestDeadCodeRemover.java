package cyr7.cfg.ir;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import cyr7.cfg.ir.nodes.CFGNode;
import cyr7.cfg.ir.nodes.CFGNodeFactory;
import cyr7.cfg.ir.nodes.CFGReturnNode;
import cyr7.cfg.ir.nodes.CFGStartNode;
import cyr7.cfg.ir.nodes.CFGStubNode;
import cyr7.cfg.ir.nodes.CFGVarAssignNode;
import cyr7.cfg.ir.opt.DeadCodeElimOptimization;
import cyr7.cfg.util.IrCfgTestUtil;
import cyr7.ir.DefaultIdGenerator;
import cyr7.ir.nodes.IRBinOp.OpType;
import cyr7.ir.nodes.IRInteger;
import cyr7.ir.nodes.IRNodeFactory_c;
import cyr7.ir.nodes.IRTemp;
import java_cup.runtime.ComplexSymbolFactory.Location;
import polyglot.util.Pair;

class TestDeadCodeRemover {

    /**
     * start <p>
     * y = 15 <p>
     * x = y <p>
     * return
     */
    @Test
    void testLinearCFG() {
        final Location loc = new Location(-1, -1);
        CFGNode returnNode = new CFGReturnNode(loc);
        CFGNode deadAssign = new CFGVarAssignNode(loc, "x",
                                    new IRTemp(loc, "y"), returnNode);
        CFGNode firstAssign = new CFGVarAssignNode(loc, "y",
                                    new IRInteger(loc, 15), deadAssign);
        CFGNode root = new CFGStartNode(loc, firstAssign);


        Set<CFGNode> expectedNodes = IrCfgTestUtil.nodeSet(
                                        root, firstAssign, returnNode);
        List<Pair<CFGNode, CFGNode>> expectedEdges = IrCfgTestUtil.edgeList(
                new Pair<>(root, firstAssign),
                new Pair<>(firstAssign, returnNode));

        CFGStartNode start = DeadCodeElimOptimization.optimize(root);

        assertTrue(IrCfgTestUtil.assertEqualGraphs(
                            start, expectedNodes, expectedEdges));

        // Running it again should remove y = 15
        expectedNodes = IrCfgTestUtil.nodeSet(root, returnNode);
        expectedEdges = IrCfgTestUtil.edgeList(new Pair<>(root, returnNode));

        start = DeadCodeElimOptimization.optimize(root);

        assertTrue(IrCfgTestUtil.assertEqualGraphs(
                start, expectedNodes, expectedEdges));
    }


    /**
     * x = 30
     * if (x > 30) {
     *    y = x
     * } else {
     *    z = x
     * }
     * println(x)
     * return
     */
    @Test
    void testBranchingCFG() {
        final Location loc = new Location(-1, -1);
        final var cfg = new CFGNodeFactory(loc);
        final var ir = new IRNodeFactory_c(loc);
        CFGNode returnNode = cfg.Return();
        CFGNode printNode = cfg.Call(
                ir.IRCallStmt(List.of(), ir.IRName("println"), List.of(ir.IRTemp("x"))),
                returnNode);

        CFGNode zIsX = cfg.VarAssign("z", ir.IRTemp("x"), printNode);
        CFGNode yIsX = cfg.VarAssign("y", ir.IRTemp("x"), printNode);

        CFGNode ifNode = cfg.If(yIsX, zIsX,
                    ir.IRBinOp(OpType.GT, ir.IRTemp("x"), ir.IRInteger(30)));

        CFGNode xAssign = cfg.VarAssign("x", ir.IRInteger(30), ifNode);
        CFGNode root = cfg.Start(xAssign);

        Set<CFGNode> expectedNodes = IrCfgTestUtil.nodeSet(root, xAssign, ifNode,
                                            printNode, returnNode);
        List<Pair<CFGNode, CFGNode>> expectedEdges = IrCfgTestUtil.edgeList(
                new Pair<>(root, xAssign),
                new Pair<>(xAssign, ifNode),
                new Pair<>(ifNode, printNode),
                new Pair<>(ifNode, printNode),
                new Pair<>(printNode, returnNode));

        CFGStartNode start = DeadCodeElimOptimization.optimize(root);

        assertTrue(IrCfgTestUtil.assertEqualGraphs(
                            start, expectedNodes, expectedEdges));
    }


    /**
     * return 5
     */
    @Test
    void testReturnValueCFG() {
        final Location loc = new Location(-1, -1);
        final var cfg = new CFGNodeFactory(loc);
        final var ir = new IRNodeFactory_c(loc);
        final var gen = new DefaultIdGenerator();
        CFGNode returnNode = cfg.Return();

        CFGNode setRV = cfg.VarAssign(gen.retTemp(0), ir.IRInteger(5), returnNode);

        CFGNode root = cfg.Start(setRV);

        Set<CFGNode> expectedNodes = IrCfgTestUtil.nodeSet(root, setRV, returnNode);
        List<Pair<CFGNode, CFGNode>> expectedEdges = IrCfgTestUtil.edgeList(
                new Pair<>(root, setRV),
                new Pair<>(setRV, returnNode));

        CFGStartNode start = DeadCodeElimOptimization.optimize(root);

        assertTrue(IrCfgTestUtil.assertEqualGraphs(
                            start, expectedNodes, expectedEdges));
    }




    /**
     * x = 0;
     * while (x < 12) {
     *    y = x
     *    x = x + 1
     * }
     * return
     */
    @Test
    void testWhileLoopCFG() {
        final Location loc = new Location(-1, -1);
        final var cfg = new CFGNodeFactory(loc);
        final var ir = new IRNodeFactory_c(loc);
        CFGNode returnNode = cfg.Return();

        CFGNode stub = new CFGStubNode();

        CFGNode xIncrement = cfg.VarAssign("x",
                ir.IRBinOp(OpType.ADD_INT, ir.IRTemp("x"), ir.IRInteger(1)),
                stub);

        CFGNode yIsX = cfg.VarAssign("y", ir.IRTemp("x"), xIncrement);

        CFGNode whileIfNode = cfg.If(yIsX, returnNode,
                ir.IRBinOp(OpType.LT, ir.IRTemp("x"), ir.IRInteger(12)));

        xIncrement.replaceOutEdge(stub, whileIfNode);

        CFGNode setX = cfg.VarAssign("x", ir.IRInteger(0), whileIfNode);

        CFGNode root = cfg.Start(setX);

        Set<CFGNode> expectedNodes = IrCfgTestUtil.nodeSet(root, setX, whileIfNode, xIncrement, returnNode);
        List<Pair<CFGNode, CFGNode>> expectedEdges = IrCfgTestUtil.edgeList(
                new Pair<>(root, setX),
                new Pair<>(setX, whileIfNode),
                new Pair<>(whileIfNode, xIncrement),
                new Pair<>(xIncrement, whileIfNode),
                new Pair<>(whileIfNode, returnNode)
                );

        CFGStartNode start = DeadCodeElimOptimization.optimize(root);

        assertTrue(IrCfgTestUtil.assertEqualGraphs(
                            start, expectedNodes, expectedEdges));
    }


    /**
     * while(true) {
     * }
     */
    @Test
    void testSurivesInfiniteLoopCFG() {
        final Location loc = new Location(-1, -1);
        final var cfg = new CFGNodeFactory(loc);

        final CFGNode loopNode = cfg.SelfLoop();
        final CFGNode root = cfg.Start(loopNode);

        final Set<CFGNode> expectedNodes = IrCfgTestUtil.nodeSet(root, loopNode);

        final List<Pair<CFGNode, CFGNode>> expectedEdges = IrCfgTestUtil.edgeList(
                new Pair<>(root, loopNode),
                new Pair<>(loopNode, loopNode));

        CFGStartNode start = DeadCodeElimOptimization.optimize(root);

        assertTrue(IrCfgTestUtil.assertEqualGraphs(
                            start, expectedNodes, expectedEdges));
    }


}
