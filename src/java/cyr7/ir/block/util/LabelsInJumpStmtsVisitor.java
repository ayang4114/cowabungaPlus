package cyr7.ir.block.util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import cyr7.ir.nodes.*;
import cyr7.visitor.MyIRVisitor;
import kotlin.NotImplementedError;
import org.jetbrains.annotations.NotNull;

public class LabelsInJumpStmtsVisitor implements MyIRVisitor<List<String>> {

    public final static LabelsInJumpStmtsVisitor instance = new LabelsInJumpStmtsVisitor();

    @Override
    public List<String> visit(IRBinOp n) {
        return List.of();
    }

    @Override
    public List<String> visit(IRCall n) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> visit(IRInteger n) {
        return List.of();
    }

    @Override
    public List<String> visit(IRESeq n) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> visit(IRMem n) {
        return List.of();
    }

    @Override
    public List<String> visit(IRTemp n) {
        return List.of();
    }

    @Override
    public List<String> visit(IRCallStmt n) {
        return List.of();
    }

    @Override
    public List<String> visit(IRName n) {
        return List.of(n.name());
    }

    @Override
    public List<String> visit(IRCJump n) {
        List<String> setOfLabels = new LinkedList<>();
        n.falseLabel().ifPresent(setOfLabels::add);
        setOfLabels.add(n.trueLabel());
        return Collections.unmodifiableList(setOfLabels);
    }

    @Override
    public List<String> visit(IRCompUnit n) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is not a statement in LIR.
     */
    @Override
    public List<String> visit(IRExp n) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is not a statement in LIR.
     */
    @Override
    public List<String> visit(IRFuncDecl n) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> visit(IRJump n) {
        List<String> labels = n.target().accept(this);
        if (labels.size() > 1) {
            throw new UnsupportedOperationException(
                    "Obtained multiple labels" + " for a jump statement.");
        }
        return labels;
    }

    @Override
    public List<String> visit(IRLabel n) {
        return List.of();
    }

    @Override
    public List<String> visit(IRMove n) {
        return List.of();
    }

    @Override
    public List<String> visit(IRReturn n) {
        return List.of();
    }

    @Override
    public List<String> visit(IRSeq n) {
        return List.of();
    }

    @Override
    public List<String> visit(@NotNull IRFloat n) {
        throw new NotImplementedError();
    }

    @Override
    public List<String> visit(@NotNull IRCast n) {
        throw new NotImplementedError();
    }
}
