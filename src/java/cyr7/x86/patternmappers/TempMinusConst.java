package cyr7.x86.patternmappers;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import cyr7.ir.nodes.IRBinOp;
import cyr7.ir.nodes.IRInteger;
import cyr7.ir.nodes.IRExpr;
import cyr7.x86.asm.ASMAddrExpr;
import cyr7.x86.asm.ASMAddrExpr.ScaleValues;
import cyr7.x86.asm.ASMArg;
import cyr7.x86.asm.ASMLine;
import cyr7.x86.asm.ASMLineFactory;
import cyr7.x86.asm.ASMTempArg;
import cyr7.x86.asm.ASMRegSize;
import cyr7.x86.pattern.BiPatternBuilder;
import cyr7.x86.tiler.ComplexTiler;

public class TempMinusConst extends MemoryAddrPattern {

    public TempMinusConst(boolean isMemPattern) {
        super(isMemPattern);
    }

    @Override
    protected Optional<ASMAddrExpr> matchAddress(
        IRBinOp n,
        ComplexTiler tiler,
        ASMLineFactory make,
        List<ASMLine> insns) {
        var tempMinusConst = BiPatternBuilder.left()
                                             .instOf(ASMTempArg.class)
                                             .right()
                                             .instOf(IRInteger.class)
                                             .and(x -> Is32Bits.check(-x.constant()))
                                             .finish()
                                             .mappingLeft(IRExpr.class,
                                                     (Function<IRExpr, ASMArg>) node -> node.accept(
                                                             tiler).result.get());

        if (tempMinusConst.matches(new Object[]
            { n.left(), n.right() })) {
            ASMTempArg lhs = tempMinusConst.leftObj();
            IRInteger rhs = tempMinusConst.rightObj();

            insns.addAll(tempMinusConst.preMapLeft()
                                       .getOptimalTiling().optimalInstructions);

            this.setCost(1
                    + tempMinusConst.preMapLeft().getOptimalTiling().tileCost);

            ASMAddrExpr addrExpr = arg.addr(Optional.of(arg.temp(lhs.name,
                    ASMRegSize.QWORD)),
                    ScaleValues.ONE,
                    Optional.empty(),
                    -rhs.constant());
            return Optional.of(addrExpr);
        } else {
            return Optional.empty();
        }
    }

}
