package cyr7.benchmark;

import java.util.Collection;
import java.util.List;

import cyr7.cli.Optimization;

public class BenchmarkLu1 extends IRBenchmark {
    @Override
    protected String filename() {
        return "benchmarks/lu/lu_benchmark1";
    }

    @Override
    Collection<? extends Optimization> testedOptimizations() {
        return List.of(Optimization.LU);
    }
}
