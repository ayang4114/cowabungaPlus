package cyr7.cfg.ir.opt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import cyr7.cfg.ir.CFGUtil;
import cyr7.cfg.ir.constructor.CFGConstructor;
import cyr7.cfg.ir.dfa.DfaResult;
import cyr7.cfg.ir.dfa.WorklistAnalysis;
import cyr7.cfg.ir.dfa.loops.BasicInductionVariableVisitor;
import cyr7.cfg.ir.dfa.loops.DominatorAnalysis;
import cyr7.cfg.ir.dfa.loops.DominatorUtil;
import cyr7.cfg.ir.nodes.CFGIfNode;
import cyr7.cfg.ir.nodes.CFGNode;
import cyr7.cfg.ir.nodes.CFGStartNode;
import cyr7.cfg.ir.nodes.CFGStubNode;
import cyr7.cli.OptConfig;
import cyr7.ir.DefaultIdGenerator;
import cyr7.ir.IRUtil;
import cyr7.ir.nodes.IRBinOp;
import cyr7.ir.nodes.IRCompUnit;
import cyr7.ir.nodes.IRBinOp.OpType;
import cyr7.ir.nodes.IRConst;
import cyr7.ir.nodes.IRExpr;
import cyr7.ir.nodes.IRTemp;
import java_cup.runtime.ComplexSymbolFactory;

public class LoopUnrollingOptimization {
    
    // The number of copies made per loop
    final static int LOOP_UNROLL_FACTOR = 5;
    
    private LoopUnrollingOptimization() {}
    
    public static CFGNode optimizeLoop(CFGNode head, Set<CFGNode> reachable) {
        BasicInductionVariableVisitor bv = new BasicInductionVariableVisitor(reachable);
        head.accept(bv);
        Map<String, Long> ivStrideMap = bv.ivStrideMap();
        
        if (reachable.size() > 15) {
            // too large of a loop
            return head;
        }
        
        if (head instanceof CFGIfNode) {
            CFGIfNode ifNode = (CFGIfNode) head;
            if (ifNode.cond instanceof IRBinOp) {
                IRBinOp guard = (IRBinOp) ifNode.cond;
                String potentialIV = null;
                OpType op = null;
                IRExpr rhs = null;
                Long potentialIVInc = null;
                if (guard.left() instanceof IRTemp) {
                    potentialIV = ((IRTemp) guard.left()).name();
                    potentialIVInc = ivStrideMap.get(potentialIV);
                }
                if (potentialIVInc != null && potentialIVInc > 0 && 
                        (guard.opType() == OpType.LT || guard.opType() == OpType.LEQ)) {
                    op = guard.opType();
                }
                
                if (potentialIVInc != null && potentialIVInc < 0 &&
                        (guard.opType() == OpType.GT || guard.opType() == OpType.GEQ)) {
                    op = guard.opType();
                }
                    
                if (guard.right() instanceof IRExpr) {
                    rhs = (IRExpr) guard.right();
                }
                if (potentialIV != null && op != null && rhs != null && ivStrideMap.containsKey(potentialIV)) {
                    CFGStubNode epilogueStub = new CFGStubNode();
                    CFGNode epilogueBody = copyNode(head.out().get(0), head, 
                            epilogueStub, new HashMap<CFGNode, CFGNode>());
                    CFGNode epilogue = head.copy(List.of(epilogueBody, head.out().get(1)));
                    for(CFGNode in: epilogueStub.in()) {
                        in.replaceOutEdge(epilogueStub, epilogue);
                    }
                    
                    CFGNode nextPointer = epilogue;
                    CFGStubNode unrollStub = new CFGStubNode();
                    for(int i = 0; i < LOOP_UNROLL_FACTOR; i++) {
                        CFGNode loopCopy;
                        if (i == 0) {
                            loopCopy = copyNode(head.out().get(0), head, 
                                unrollStub, new HashMap <CFGNode, CFGNode>());
                        } else {
                            loopCopy = copyNode(head.out().get(0), head, 
                                    nextPointer, new HashMap <CFGNode, CFGNode>());
                        }
                        nextPointer = loopCopy;
                    }
                    
                    ComplexSymbolFactory.Location l = ifNode.cond.location();
                    
                    // i + c(n-1)
                    IRExpr newLHS = new IRBinOp(l, OpType.ADD, new IRTemp(l, potentialIV), 
                            new IRBinOp(l, OpType.MUL, new IRConst(l, ivStrideMap.get(potentialIV)), 
                                    new IRConst(l, LOOP_UNROLL_FACTOR - 1)));
                    // i + c(n-1) < u
                    IRExpr newCond = new IRBinOp(l, op, newLHS, rhs);
                    CFGNode unrolledHeader = new CFGIfNode(head.location(), epilogue, nextPointer, newCond);
                    
                    for(CFGNode in: unrollStub.in()) {
                        in.replaceOutEdge(unrollStub, unrolledHeader);
                    }
                    
                    return unrolledHeader;
                }
            }
        }
        return head;
    }
    
    // headRef is used to find the last node in this iteration of the loop, 
    // and have it point to [nextPointer]
    private static CFGNode copyNode(CFGNode toCopy, CFGNode headRef, 
            CFGNode nextPointer, Map<CFGNode, CFGNode> copies) {
        if (copies.containsKey(toCopy)) {
            return copies.get(toCopy);
        }
        List<CFGNode> copiedOutNodes = new ArrayList<CFGNode>();
        List<CFGStubNode> stubNodes = new ArrayList<CFGStubNode>();
        for (CFGNode out: toCopy.out()) {
            if (out == headRef) {;
                copiedOutNodes.add(nextPointer);
            } else if (copies.containsKey(out)) {
                copiedOutNodes.add(copies.get(out));
            } else {
                CFGStubNode copyStub = new CFGStubNode();
                copies.put(toCopy, copyStub);
                stubNodes.add(copyStub);
                copiedOutNodes.add(copyNode(out, headRef, nextPointer, copies));
            }
        }
        CFGNode copy = toCopy.copy(copiedOutNodes);
        for(CFGStubNode stub: stubNodes) {
            for(CFGNode in: stub.in()) {
                in.replaceOutEdge(stub, copy);
            }
        }
        copies.put(toCopy, copy);
        return copy;
        
    }
    
    public static CFGStartNode optimize(CFGNode start) {
        CFGStartNode startNode = (CFGStartNode)start;
        DfaResult<Set<CFGNode>> result =
                WorklistAnalysis.analyze(startNode, DominatorAnalysis.INSTANCE);

        Map<CFGNode, Set<CFGNode>> cleanedDominators = DominatorUtil.generateMap(result.out());
        runIVAnalysis(cleanedDominators);
        return startNode;
    }
    
    public static void runIVAnalysis(
            Map<CFGNode, Set<CFGNode>> dominators) {
        
        Set<CFGNode> nodesAnalyzed = new HashSet<>();
        for(Map.Entry<CFGNode, Set<CFGNode>> pair: dominators.entrySet()) {
            CFGNode node = pair.getKey();
            for(CFGNode out: node.out()) {
                // If there is an out edge to a dominator of this node, there's a loop
                if (pair.getValue().contains(out) && !nodesAnalyzed.contains(out)) {
                    Set<CFGNode> tailNodes = new HashSet<>();
                    for (CFGNode tailNode: out.in()) {
                        // take union of backwards search from all tailnodes from the head
                        // Need to check if contains because it is possible it was replaced
                        if (dominators.containsKey(tailNode) && dominators.get(tailNode).contains(out)) {
                            tailNodes.add(tailNode);
                        }
                    }
                    Set<CFGNode> reachable = backwardsSearch(tailNodes, out);
                    nodesAnalyzed.addAll(reachable);
                    CFGNode newUnrolledHead = LoopUnrollingOptimization.optimizeLoop(out, reachable);
                    for(CFGNode inc: out.in()) {
                        if (!reachable.contains(inc)) {
                            inc.replaceOutEdge(out, newUnrolledHead);
                        }
                    }

                }
            }
        }
    }
    
    // Precondition: tail is dominated by head.
    public static Set<CFGNode> backwardsSearch(Set<CFGNode> tailNodes, CFGNode head) {
        Set<CFGNode> reachable = new HashSet<>();
        Stack<CFGNode> nodes = new Stack<>();
        for(CFGNode tail: tailNodes) {
            nodes.push(tail);
        }
        while(nodes.size() > 0) {
            CFGNode next = nodes.pop();
            for (CFGNode in: next.in()) {
                if (in != head && !reachable.contains(in)) {
                    nodes.push(in);
                }
            }
            reachable.add(next);
        }
        reachable.add(head);
        return reachable;
    }
    
    
    public static void main(String[] args) throws Exception {
        File f = new File("tests/resources/testJunk.xi");
        FileReader fr = new FileReader(f);
        BufferedReader br  = new BufferedReader(fr);
        Reader reader = new BufferedReader(br);
        DefaultIdGenerator idGenerator = new DefaultIdGenerator();
        IRCompUnit lowered = IRUtil.generateIR(
            reader,
            "testJunk.xi",
            null,
            OptConfig.none(),

            new DefaultIdGenerator());
        Map<String, CFGStartNode> cfgResult = CFGConstructor.constructCFG(lowered);
        CFGStartNode start = cfgResult.get("_Imain_paai");
        optimize(start);
        
        Writer writer = new PrintWriter(System.out);
        CFGUtil.outputDotForFunctionIR(start, writer);
         
    }
}