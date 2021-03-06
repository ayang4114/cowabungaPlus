package cyr7.cfg.ir.opt;

import java.util.Map;

import cyr7.ir.nodes.*;
import cyr7.visitor.MyIRVisitor;
import kotlin.NotImplementedError;
import org.jetbrains.annotations.NotNull;

public class IRTempReplacer {

    private IRTempReplacer() {}

    /**
     * Replaces all variables {@code v} used in {@code expr} with String to
     * String {@code mapping}. It is possible that variable substitution
     * requires several traversals through the map.
     * <p>
     * For example, suppose {@code mapping} contains the mappings
     * {x --> y}, {a --> x}, and {@code expr} is {@code a}. Then, {@code expr}
     * will be replaced as {@code y}. First, it gets {@code x}, but then
     * {@code x} is also mapped to {@code y}. At last, there is no further
     * mapping for {@code y}.
     *
     * @param expr The expression that may have temporaries to be replaced.
     * @param mapping Requires: there cannot be looped mappings, i.e. a mapping
     *                of {a --> b} and {b --> a} cannot be in {@code mapping}.
     * @return The expression {@code expr} with temporaries replaced.
     */
    public static IRExpr replace(IRExpr expr, Map<String, String> mapping) {
        return expr.accept(new IRReplaceTempVisitor(mapping));
    }

    private static class IRReplaceTempVisitor implements MyIRVisitor<IRExpr> {

        private final Map<String, String> mapping;

        public IRReplaceTempVisitor(Map<String, String> mapping) {
            this.mapping = mapping;
        }

        @Override
        public IRExpr visit(IRBinOp n) {
            var replaceLeft = n.left().accept(this);
            var replaceRight = n.right().accept(this);

            return new IRBinOp(n.location(), n.opType(), replaceLeft, replaceRight);
        }

        @Override
        public IRExpr visit(IRCall n) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IRExpr visit(IRInteger n) {
            return n;
        }

        @Override
        public IRExpr visit(IRESeq n) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IRExpr visit(IRMem n) {
            return new IRMem(n.location(), n.expr().accept(this));
        }

        @Override
        public IRExpr visit(IRName n) {
            return n;
        }

        /**
         * Traverses to the oldest copy.
         */
        @Override
        public IRExpr visit(IRTemp n) {
            String key = n.name();
            while (this.mapping.containsKey(key)) {
                key = this.mapping.get(key);
            }
            return new IRTemp(n.location(), key);
        }

        @Override
        public IRExpr visit(IRCallStmt n) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IRExpr visit(IRCJump n) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IRExpr visit(IRCompUnit n) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IRExpr visit(IRExp n) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IRExpr visit(IRFuncDecl n) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IRExpr visit(IRJump n) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IRExpr visit(IRLabel n) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IRExpr visit(IRMove n) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IRExpr visit(IRReturn n) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IRExpr visit(IRSeq n) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IRExpr visit(@NotNull IRFloat n) {
            throw new NotImplementedError();
        }

        @Override
        public IRExpr visit(@NotNull IRCast n) {
            throw new NotImplementedError();
        }
    }

}
