package cyr7.x86.tiler;

import cyr7.ir.IdGenerator;
import cyr7.ir.nodes.*;
import cyr7.semantics.types.FunctionType;
import cyr7.visitor.MyIRVisitor;
import cyr7.x86.asm.ASMArgFactory;
import cyr7.x86.asm.ASMInstr;
import cyr7.x86.asm.ASMLineFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ComplexTiler implements MyIRVisitor<List<ASMInstr>> {

    @Override
    public List<ASMInstr> visit(IRBinOp n) {
        return null;
    }

    @Override
    public List<ASMInstr> visit(IRCall n) {
        return null;
    }

    @Override
    public List<ASMInstr> visit(IRConst n) {
        return null;
    }

    @Override
    public List<ASMInstr> visit(IRESeq n) {
        return null;
    }

    @Override
    public List<ASMInstr> visit(IRMem n) {
        return null;
    }

    @Override
    public List<ASMInstr> visit(IRName n) {
        return null;
    }

    @Override
    public List<ASMInstr> visit(IRTemp n) {
        return null;
    }

    @Override
    public List<ASMInstr> visit(IRCallStmt n) {
        return null;
    }

    @Override
    public List<ASMInstr> visit(IRCJump n) {
        return null;
    }

    @Override
    public List<ASMInstr> visit(IRCompUnit n) {
        return null;
    }

    @Override
    public List<ASMInstr> visit(IRExp n) {
        return null;
    }

    @Override
    public List<ASMInstr> visit(IRFuncDecl n) {
        return null;
    }

    @Override
    public List<ASMInstr> visit(IRJump n) {
        return null;
    }

    @Override
    public List<ASMInstr> visit(IRLabel n) {
        return null;
    }

    @Override
    public List<ASMInstr> visit(IRMove n) {
        return null;
    }

    @Override
    public List<ASMInstr> visit(IRReturn n) {
        return null;
    }

    @Override
    public List<ASMInstr> visit(IRSeq n) {
        return null;
    }
}