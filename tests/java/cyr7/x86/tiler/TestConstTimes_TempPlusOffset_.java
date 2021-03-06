package cyr7.x86.tiler;

import cyr7.ir.nodes.IRBinOp;
import cyr7.ir.nodes.IRBinOp.OpType;
import org.junit.jupiter.api.Test;

import static cyr7.x86.tiler.ASMTestUtils.assertEqualsTiled;
import static cyr7.x86.tiler.ASMTestUtils.makeIR;

public class TestConstTimes_TempPlusOffset_ {

    @Test
    void testConstTimesTempAndOffset() {
        IRBinOp constTempOffset = makeIR(make ->
                make.IRBinOp(OpType.MUL_INT,
                        make.IRBinOp(OpType.ADD_INT,
                                make.IRInteger(7),
                                make.IRTemp("bleh")),
                        make.IRInteger(4))
        );

        assertEqualsTiled(constTempOffset, "leaq _t0, [ 4 * bleh + 28 ]");
    }

    @Test
    void testConstTimesOffsetAndTemp() {
        IRBinOp constTempOffset = makeIR(make ->
                make.IRBinOp(OpType.MUL_INT,
                        make.IRBinOp(OpType.ADD_INT,
                                make.IRTemp("bleh"),
                                make.IRInteger(2)),
                        make.IRInteger(8))
        );

        assertEqualsTiled(constTempOffset, "leaq _t0, [ 8 * bleh + 16 ]");
    }

    @Test
    void testOffsetAndConstTimesTemp() {
        IRBinOp constTempOffset = makeIR(make ->
                make.IRBinOp(OpType.MUL_INT,
                        make.IRInteger(1),
                        make.IRBinOp(OpType.ADD_INT,
                                make.IRInteger(3),
                                make.IRTemp("bleh"))
                )
        );

        assertEqualsTiled(constTempOffset, "leaq _t0, [ 1 * bleh + 3 ]");
    }

    @Test
    void testOffsetAndTempTimesConstant() {
        IRBinOp constTempOffset = makeIR(make ->
                make.IRBinOp(OpType.MUL_INT,
                        make.IRInteger(4),
                        make.IRBinOp(OpType.ADD_INT,
                                make.IRTemp("bleh"),
                                make.IRInteger(3))
                )
        );

        assertEqualsTiled(constTempOffset, "leaq _t0, [ 4 * bleh + 12 ]");
    }

    @Test
    void testConstTimesTempAndOffsetOver32Bits() {
        IRBinOp constTempOffset = makeIR(make ->
            make.IRBinOp(OpType.MUL_INT,
                make.IRBinOp(OpType.ADD_INT,
                    make.IRInteger(1099511627776L),
                    make.IRTemp("bleh")),
                make.IRInteger(4))
        );

        assertEqualsTiled(constTempOffset,
            "movq _t0, 1099511627776",
            "leaq _t1, [ _t0 + 1 * bleh ]",
            "leaq _t2, [ 4 * _t1 ]");
    }

    @Test
    void testConstOver32BitsTimesOffsetAndTemp() {
        IRBinOp constTempOffset = makeIR(make ->
            make.IRBinOp(OpType.MUL_INT,
                make.IRBinOp(OpType.ADD_INT,
                    make.IRTemp("bleh"),
                    make.IRInteger(1099511627776L)),
                make.IRInteger(8))
        );

        assertEqualsTiled(constTempOffset,
            "movq _t0, 1099511627776",
            "leaq _t1, [ bleh + 1 * _t0 ]",
            "leaq _t2, [ 8 * _t1 ]");
    }

    @Test
    void testOffsetAndConstOver32BitsTimesTemp() {
        IRBinOp constTempOffset = makeIR(make ->
            make.IRBinOp(OpType.MUL_INT,
                make.IRInteger(1),
                make.IRBinOp(OpType.ADD_INT,
                    make.IRInteger(1099511627776L),
                    make.IRTemp("bleh"))
            )
        );

        assertEqualsTiled(constTempOffset,
            "movq _t0, 1099511627776",
            "leaq _t1, [ _t0 + 1 * bleh ]",
            "leaq _t2, [ 1 * _t1 ]");
    }

    @Test
    void testOffsetAndTempTimesConstOver32Bits() {
        IRBinOp constTempOffset = makeIR(make ->
            make.IRBinOp(OpType.MUL_INT,
                make.IRInteger(4),
                make.IRBinOp(OpType.ADD_INT,
                    make.IRTemp("bleh"),
                    make.IRInteger(1099511627776L))
            )
        );

        assertEqualsTiled(constTempOffset,
            "movq _t0, 1099511627776",
            "leaq _t1, [ bleh + 1 * _t0 ]",
            "leaq _t2, [ 4 * _t1 ]");
    }

}
