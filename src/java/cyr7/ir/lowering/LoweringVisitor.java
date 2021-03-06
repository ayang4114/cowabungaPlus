package cyr7.ir.lowering;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import cyr7.ir.IdGenerator;
import cyr7.ir.lowering.LoweringVisitor.Result;
import cyr7.ir.nodes.*;
import cyr7.ir.visit.CheckCanonicalIRVisitor;
import cyr7.util.OneOfThree;
import cyr7.visitor.MyIRVisitor;
import kotlin.NotImplementedError;
import org.jetbrains.annotations.NotNull;
import polyglot.util.Pair;

public class LoweringVisitor implements MyIRVisitor<Result> {

    @Override
    public Result visit(@NotNull IRFloat n) {
        throw new NotImplementedError();
    }

    @Override
    public Result visit(@NotNull IRCast n) {
        throw new NotImplementedError();
    }

    public static class Result extends
            OneOfThree<List<IRStmt>, Pair<List<IRStmt>, IRExpr>, IRCompUnit> {

        /**
         * Creates a statement result, based on the output when lowering
         * statements.
         *
         * @param statement A list of statements whose child expressions are
         *                  pure expressions.
         * @return
         */
        public static Result stmts(List<IRStmt> statement) {
            return new Result(statement, null, null);
        }

        /**
         * Creates an expression result, based on the output when lowering
         * expressions. This consists of a list of statements and a pure
         * expression.
         *
         * @param sideEffects The list of statements that performs the same
         *                    computations as the side effects in an inpure
         *                    expression.
         * @param expr        A pure expression, i.e. no side effects.
         * @return
         */
        public static Result expr(List<IRStmt> sideEffects, IRExpr expr) {
            return new Result(null, new Pair<>(sideEffects, expr), null);
        }

        public static Result compUnit(IRCompUnit compUnit) {
            return new Result(null, null, compUnit);
        }

        protected Result(List<IRStmt> stmts, Pair<List<IRStmt>, IRExpr> expr,
                IRCompUnit compUnit) {
            super(stmts, expr, compUnit);
        }

    }

    /**
     * Generates temporary names, argument temps, return value temps, and label
     * names. No two names created by generator are the same.
     */
    private final IdGenerator generator;

    public LoweringVisitor(IdGenerator generator) {
        this.generator = generator;
    }

    // Visitor

    @Override
    public Result visit(IRBinOp n) {
        IRNodeFactory make = new IRNodeFactory_c(n.location());
        var leftResult = n.left().accept(this).assertSecond();
        var rightResult = n.right().accept(this).assertSecond();
        List<IRStmt> stmts = new ArrayList<>();

        stmts.addAll(leftResult.part1());
        IRExpr lhs = leftResult.part2();

        CheckCanonicalIRVisitor cv = new CheckCanonicalIRVisitor();
        if (!cv.visit(n.right())){
            // if the RHS has side effects
            IRTemp t1 = make.IRTemp(generator.newTemp());
            stmts.add(make.IRMove(t1, leftResult.part2()));
            lhs = t1;
        }
        stmts.addAll(rightResult.part1());

        IRExpr expr = make.IRBinOp(n.opType(), lhs, rightResult.part2());
        return Result.expr(stmts, expr);
    }

    @Override
    public Result visit(IRCall n) {
        IRNodeFactory make = new IRNodeFactory_c(n.location());
        List<IRStmt> stmts = new LinkedList<>();

        // List of temps representing the values of each function argument
        List<IRExpr> argValTemps = new ArrayList<>();
        for (IRExpr arg : n.args()) {
            Result argResult = arg.accept(this);
            var resultPair = argResult.assertSecond();
            stmts.addAll(resultPair.part1());

            IRTemp argValTemp = make.IRTemp(generator.newTemp());
            argValTemps.add(argValTemp);
            // Cannot move directly into ARG_0 as nested function calls will
            // overwrite the value
            // i.e. call(1, call(0, 0))
            stmts.add(make.IRMove(argValTemp, resultPair.part2()));
        }

        String result = generator.newTemp();
        stmts.add(make.IRCallStmt(List.of(result), n.target(), argValTemps));
        return Result.expr(stmts, make.IRTemp(result));
    }

    @Override
    public Result visit(IRInteger n) {
        return Result.expr(List.of(), n);
    }

    @Override
    public Result visit(IRESeq n) {
        List<IRStmt> stmts = new ArrayList<>();
        stmts.addAll(n.stmt().accept(this).assertFirst());
        var result = n.expr().accept(this).assertSecond();
        stmts.addAll(result.part1());
        return Result.expr(stmts, result.part2());
    }

    @Override
    public Result visit(IRMem n) {
        IRNodeFactory make = new IRNodeFactory_c(n.location());
        var index = n.expr().accept(this).assertSecond();
        return Result.expr(index.part1(), make.IRMem(index.part2()));
    }

    @Override
    public Result visit(IRName n) {
        return Result.expr(List.of(), n);
    }

    @Override
    public Result visit(IRTemp n) {
        return Result.expr(List.of(), n);
    }

    @Override
    public Result visit(IRCallStmt n) {
        IRNodeFactory make = new IRNodeFactory_c(n.location());

        List<IRStmt> stmts = new LinkedList<>();

        // List of temps representing the values of each function argument
        List<IRExpr> argValTemps = new ArrayList<>();
        for (IRExpr arg : n.args()) {
            Result argResult = arg.accept(this);
            var resultPair = argResult.assertSecond();
            stmts.addAll(resultPair.part1());

            IRTemp argValTemp = make.IRTemp(generator.newTemp());
            argValTemps.add(argValTemp);
            // Cannot move directly into ARG_0 as nested function calls will
            // overwrite the value
            // i.e. call(1, call(0, 0))
            stmts.add(make.IRMove(argValTemp, resultPair.part2()));
        }

        stmts.add(make.IRCallStmt(n.collectors(), n.target(), argValTemps));
        return Result.stmts(stmts);
    }

    @Override
    public Result visit(IRCJump n) {
        IRNodeFactory make = new IRNodeFactory_c(n.location());
        var index = n.cond().accept(this).assertSecond();

        List<IRStmt> stmts = new ArrayList<>(index.part1());
        stmts.add(make.IRCJump(index.part2(), n.trueLabel(), n.falseLabel()));

        return Result.stmts(stmts);
    }

    @Override
    public Result visit(IRCompUnit n) {
        IRNodeFactory make = new IRNodeFactory_c(n.location());
        IRCompUnit compUnit = make.IRCompUnit(n.name());
        var functions = n.functions().values();
        for (IRFuncDecl funcDecl : functions) {
            List<IRStmt> loweredStmts = funcDecl.body().accept(this)
                    .assertFirst();
            IRStmt loweredBody = make.IRSeq(loweredStmts);
            IRFuncDecl loweredFuncDecl = make.IRFuncDecl(funcDecl.name(),
                    loweredBody, funcDecl.type());
            compUnit.appendFunc(loweredFuncDecl);
        }
        return Result.compUnit(compUnit);
    }

    @Override
    public Result visit(IRExp n) {
        return Result.stmts(n.expr().accept(this).assertSecond().part1());
    }

    @Override
    public Result visit(IRFuncDecl n) {
        IRNodeFactory make = new IRNodeFactory_c(n.location());
        List<IRStmt> stmts = new LinkedList<>();

        stmts.add(make.IRLabel(n.name()));
        var body = n.body().accept(this).assertFirst();
        stmts.addAll(body);
        return Result.stmts(stmts);
    }

    @Override
    public Result visit(IRJump n) {
        IRNodeFactory make = new IRNodeFactory_c(n.location());
        var index = n.target().accept(this).assertSecond();

        List<IRStmt> stmts = new ArrayList<>(index.part1());
        stmts.add(make.IRJump(index.part2()));

        return Result.stmts(stmts);
    }

    @Override
    public Result visit(IRLabel n) {
        return Result.stmts(List.of(n));
    }

    @Override
    public Result visit(IRReturn n) {
        return Result.stmts(List.of(n));
    }

    @Override
    public Result visit(IRMove n) {
        IRNodeFactory make = new IRNodeFactory_c(n.target().location());

        var targetResult = n.target().accept(this).assertSecond();
        var targetSideEffects = targetResult.part1();
        var targetExpr = targetResult.part2();

        var sourceResult = n.source().accept(this).assertSecond();
        var sourceSideEffects = sourceResult.part1();
        var sourceExpr = sourceResult.part2();

        if (targetExpr instanceof IRTemp) {
            List<IRStmt> stmts = new ArrayList<>();

            // add the target side effects to the overall side effects, but we
            // really expect it to be empty
            assert targetSideEffects.isEmpty();
            stmts.addAll(targetSideEffects);

            stmts.addAll(sourceSideEffects);
            stmts.add(make.IRMove(targetExpr, sourceExpr));

            return Result.stmts(stmts);

        } else if (targetExpr instanceof IRMem) {
            IRMem targetMemExpr = (IRMem) targetExpr;
            IRTemp t = make.IRTemp(generator.newTemp());

            List<IRStmt> stmts = new ArrayList<>();
            stmts.addAll(targetSideEffects);
            stmts.add(make.IRMove(t, targetMemExpr.expr()));
            stmts.addAll(sourceSideEffects);
            stmts.add(make.IRMove(make.IRMem(t), sourceExpr));

            return Result.stmts(stmts);

        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Result visit(IRSeq n) {
        return Result.stmts(n.stmts().stream()
                .flatMap(s -> s.accept(this).assertFirst().stream())
                .collect(Collectors.toList()));
    }

}
