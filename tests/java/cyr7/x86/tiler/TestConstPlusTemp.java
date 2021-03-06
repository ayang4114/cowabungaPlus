package cyr7.x86.tiler;

import cyr7.ir.nodes.IRBinOp;
import cyr7.ir.nodes.IRBinOp.OpType;
import org.junit.jupiter.api.Test;

import static cyr7.x86.tiler.ASMTestUtils.assertEqualsTiled;
import static cyr7.x86.tiler.ASMTestUtils.makeIR;

public class TestConstPlusTemp {

    @Test
    void testConstPlusTemp() {
        IRBinOp constTemp = makeIR(make ->
            make.IRBinOp(
                OpType.ADD_INT,
                make.IRInteger(50),
                make.IRTemp("bleh")
            ));

        assertEqualsTiled(constTemp, "leaq _t0, [ bleh + 50 ]");
    }

    @Test
    void testTempPlusConst() {
        IRBinOp constTemp = makeIR(make ->
            make.IRBinOp(
                OpType.ADD_INT,
                make.IRTemp("bleh"),
                make.IRInteger(50)
            ));

        assertEqualsTiled(constTemp, "leaq _t0, [ bleh + 50 ]");
    }

    @Test
    void testTempPlusConstOver32Bits() {
        IRBinOp constTemp = makeIR(make ->
            make.IRBinOp(
                OpType.ADD_INT,
                make.IRTemp("bleh"),
                make.IRInteger(1099511627776L)
            ));

        assertEqualsTiled(constTemp,
            "movq _t0, 1099511627776",
            "leaq _t1, [ bleh + 1 * _t0 ]");
    }

}
