package cyr7.x86.tiler;

import cyr7.ir.IdGenerator;
import cyr7.ir.nodes.IRBinOp;
import cyr7.ir.nodes.IRConst;
import cyr7.ir.nodes.IRNode;
import cyr7.x86.asm.ASMAddrExpr.ScaleValues;
import cyr7.x86.asm.ASMArg;
import cyr7.x86.asm.ASMLine;
import cyr7.x86.asm.ASMLineFactory;
import cyr7.x86.asm.ASMTempArg;
import cyr7.x86.asm.ASMTempArg.Size;
import cyr7.x86.pattern.BiPatternBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ComplexTiler extends BasicTiler {

    private static final Comparator<TilerData> byCost
        = Comparator.comparingInt(lhs -> lhs.tileCost);

    public ComplexTiler(IdGenerator generator, int numRetValues,
                        String returnLbl,
                        Optional<ASMTempArg> additionalRetValAddress,
                        boolean stack16ByteAligned) {
        super(generator, numRetValues, returnLbl, additionalRetValAddress,
            stack16ByteAligned);

        disableBasicTilerMemoizeResults();
    }

    @Override
    public TilerData visit(IRBinOp n) {
        ASMLineFactory make = new ASMLineFactory(n);
        if (n.hasOptimalTiling()) {
            return n.getOptimalTiling();
        }

        List<TilerData> possibleTilings = new ArrayList<>();

        switch (n.opType()) {
            case MUL:
                var pattern = BiPatternBuilder
                    .left()
                    .instOf(IRConst.class)
                    .and(x -> x.constant() == 1 || x.constant() == 2 || x.constant() == 4 || x.constant() == 8)
                    .right()
                    .instOf(ASMTempArg.class)
                    .finish()
                    .mappingRight(IRNode.class, (Function<IRNode, ASMArg>) node -> node.accept(this).result.get())
                    .enableCommutes();

                ASMTempArg resultTemp = arg.temp(generator.newTemp(), Size.QWORD);

                if (pattern.matches(new Object[] { n.left(), n.right() })) {
                    IRConst constArg = pattern.leftObj();
                    ASMTempArg tempArg = pattern.rightObj();

                    ASMLine line = make.Lea(
                        resultTemp,
                        arg.mem(arg.addr(
                            Optional.empty(),
                            ScaleValues.fromConst(constArg.constant()).get(),
                            Optional.of(arg.temp(tempArg.name, Size.QWORD)),
                            0
                        ))
                    );

                    possibleTilings.add(
                        new TilerData(1,
                            List.of(line),
                            Optional.of(resultTemp)
                        ));
                }
        }

        possibleTilings.add(super.visit(n));

        TilerData optimal = possibleTilings.stream().min(byCost).get();
        n.setOptimalTilingOnce(optimal);
        return optimal;
    }

}
