package cyr7.ir.interpret.builtin.assert

import cyr7.ir.interpret.builtin.LibraryFunction
import cyr7.ir.interpret.SimulatorSettings
import cyr7.ir.interpret.exception.Trap

class Assert(settings: SimulatorSettings): LibraryFunction(settings) {
    override fun callName() = "_Iassert_pb"
    override fun moduleName() = "assert"
    override fun execute(args: LongArray): List<Long> {
        if (args[0] != 1L) throw Trap("Assertion error!")
        else return emptyList()
    }
}
