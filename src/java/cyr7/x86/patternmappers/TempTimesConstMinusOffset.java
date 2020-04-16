package cyr7.x86.patternmappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import cyr7.ir.nodes.IRBinOp;
import cyr7.ir.nodes.IRBinOp.OpType;
import cyr7.ir.nodes.IRConst;
import cyr7.ir.nodes.IRExpr;
import cyr7.x86.asm.ASMAddrExpr;
import cyr7.x86.asm.ASMAddrExpr.ScaleValues;
import cyr7.x86.asm.ASMArg;
import cyr7.x86.asm.ASMLine;
import cyr7.x86.asm.ASMLineFactory;
import cyr7.x86.asm.ASMTempArg;
import cyr7.x86.asm.ASMTempArg.Size;
import cyr7.x86.pattern.BiPatternBuilder;
import cyr7.x86.tiler.ComplexTiler;
import cyr7.x86.tiler.TilerData;

public class TempTimesConstMinusOffset extends MemoryAddrPattern {

    public TempTimesConstMinusOffset(boolean isMemPattern) {
        super(isMemPattern);
    }

    @Override
    public Optional<TilerData> match(IRBinOp n, ComplexTiler tiler,
            ASMLineFactory make) {
        if (n.opType() != OpType.SUB) {
            return Optional.empty();
        }

        var constTemp = BiPatternBuilder.left()
                                        .instOf(IRConst.class)
                                        .and(x -> x.constant() == 1 || x.constant() == 2
                                                || x.constant() == 4 || x.constant() == 8)
                                        .right()
                                        .instOf(ASMTempArg.class)
                                        .finish()
                                        .mappingRight(IRExpr.class,
                                                (Function<IRExpr, ASMArg>) node -> node.accept(
                                                        tiler).result.get())
                                        .enableCommutes();

        var constTempMinusN = BiPatternBuilder.left()
                                             .instOf(IRBinOp.class)
                                             .and(x -> x.opType() == OpType.MUL)
                                             .and(x -> constTemp.matches(
                                                     new Object[]
                                                     { x.left(), x.right() }))
                                             .right()
                                             .instOf(IRConst.class)
                                             .finish()
                                             .enableCommutes();

        if (constTempMinusN.matches(new Object[]
            { n.left(), n.right() })) {
            IRConst constArg = constTemp.leftObj();
            ASMTempArg tempArg = constTemp.rightObj();
            IRConst nArg = constTempMinusN.rightObj();

            List<ASMLine> insns = new ArrayList<>();
            insns.addAll(constTemp.preMapRight()
                                  .getOptimalTiling().optimalInstructions);

            ASMAddrExpr addrExpr = arg.addr(Optional.empty(),
                    ScaleValues.fromConst(constArg.constant())
                               .get(),
                    Optional.of(arg.temp(tempArg.name, Size.QWORD)),
                    -nArg.constant());
            if (this.isMemPattern) {
                return Optional.of(new TilerData(1, insns,
                        Optional.of(arg.mem(
                        addrExpr))));
            } else {
                ASMTempArg resultTemp = arg.temp(tiler.generator()
                                                      .newTemp(), Size.QWORD);
                ASMLine line = make.Lea(resultTemp, arg.mem(addrExpr));
                insns.add(line);
                return Optional.of(new TilerData(1, insns, Optional.of(
                        resultTemp)));
            }
        }

        return Optional.empty();
    }

}