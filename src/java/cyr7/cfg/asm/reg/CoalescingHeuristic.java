package cyr7.cfg.asm.reg;

import cyr7.x86.asm.ASMTempRegArg;

import java.util.Set;

final class CoalescingHeuristic {

    private final int K;
    private final Set<ASMTempRegArg> precolored;
    private final InterferenceGraph graph;

    public CoalescingHeuristic(
        int K,
        Set<ASMTempRegArg> precolored,
        InterferenceGraph graph) {

        this.K = K;
        this.precolored = precolored;
        this.graph = graph;
    }

    public boolean ok(ASMTempRegArg t, ASMTempRegArg r) {
        return graph.degree(t) < K - 1
            || precolored.contains(t)
            || graph.adjacentSet(t, r);
    }

    public boolean conservative(Set<ASMTempRegArg> nodes) {
        int k = 0;
        for (ASMTempRegArg n : nodes) {
            if (graph.degree(n) >= K) {
                k++;
            }
        }
        return k < K;
    }

}
