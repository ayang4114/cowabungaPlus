package cyr7.x86.patternmappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import cyr7.ir.nodes.IRBinOp;
import cyr7.ir.nodes.IRConst;
import cyr7.ir.nodes.IRExpr;
import cyr7.x86.asm.ASMAddrExpr;
import cyr7.x86.asm.ASMAddrExpr.ScaleValues;
import cyr7.x86.asm.ASMArg;
import cyr7.x86.asm.ASMLine;
import cyr7.x86.asm.ASMLineFactory;
import cyr7.x86.asm.ASMMemArg;
import cyr7.x86.asm.ASMTempArg;
import cyr7.x86.asm.ASMTempArg.Size;
import cyr7.x86.pattern.BiPatternBuilder;
import cyr7.x86.tiler.ComplexTiler;
import cyr7.x86.tiler.TilerData;

public class TempMinusConst extends MemoryAddrPattern {

    public TempMinusConst(boolean isMemPattern) {
        super(isMemPattern);
    }

    @Override
    public Optional<TilerData> match(IRBinOp n, ComplexTiler tiler,
            ASMLineFactory make) {
        var tempMinusConst = BiPatternBuilder.left()
                                             .instOf(ASMTempArg.class)
                                             .right()
                                             .instOf(IRConst.class)
                                             .finish()
                                             .mappingLeft(IRExpr.class,
                                                     (Function<IRExpr, ASMArg>) node -> node.accept(
                                                             tiler).result.get());

        if (tempMinusConst.matches(new Object[]
            { n.left(), n.right() })) {
            ASMTempArg lhs = tempMinusConst.leftObj();
            IRConst rhs = tempMinusConst.rightObj();

            List<ASMLine> insns = new ArrayList<>();
            insns.addAll(tempMinusConst.preMapLeft()
                                       .getOptimalTiling().optimalInstructions);

            ASMAddrExpr addrExpr = arg.addr(Optional.of(arg.temp(lhs.name,
                    Size.QWORD)),
                    ScaleValues.ONE,
                    Optional.empty(),
                    -rhs.constant());
            if (this.isMemPattern) {
                return Optional.of(new TilerData(0, insns, Optional
                        .of(
                    new ASMMemArg(addrExpr))));
            } else {
                ASMTempArg resultTemp = arg.temp(tiler.generator().newTemp(),
                        Size.QWORD);
                ASMLine line = make.Lea(resultTemp, arg.mem(addrExpr));
                insns.add(line);
                return Optional
                        .of(new TilerData(1, insns, Optional.of(resultTemp)));
            }
        } else {
            return Optional.empty();
        }
    }

}