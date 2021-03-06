package cyr7.benchmark;

import cyr7.cli.Optimization;

import java.util.Collection;
import java.util.List;

public class BenchmarkCopyDce3 extends IRBenchmark {
    @Override
    protected String filename() {
        return "benchmarks/copy_dce/copy_dce_benchmark3";
    }

    @Override
    Collection<? extends Optimization> testedOptimizations() {
        return List.of(Optimization.DCE, Optimization.COPY);
    }

}
