package cyr7.ir.fold.basic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cyr7.ir.fold.visitors.BinopCalc;
import cyr7.ir.nodes.IRBinOp;
import cyr7.ir.nodes.IRBinOp.OpType;
import cyr7.ir.nodes.IRCJump;
import cyr7.ir.nodes.IRCall;
import cyr7.ir.nodes.IRCallStmt;
import cyr7.ir.nodes.IRCompUnit;
import cyr7.ir.nodes.IRConst;
import cyr7.ir.nodes.IRESeq;
import cyr7.ir.nodes.IRExp;
import cyr7.ir.nodes.IRExpr;
import cyr7.ir.nodes.IRFuncDecl;
import cyr7.ir.nodes.IRJump;
import cyr7.ir.nodes.IRLabel;
import cyr7.ir.nodes.IRMem;
import cyr7.ir.nodes.IRMove;
import cyr7.ir.nodes.IRName;
import cyr7.ir.nodes.IRNodeFactory;
import cyr7.ir.nodes.IRNodeFactory_c;
import cyr7.ir.nodes.IRReturn;
import cyr7.ir.nodes.IRSeq;
import cyr7.ir.nodes.IRStmt;
import cyr7.ir.nodes.IRTemp;
import cyr7.util.OneOfThree;
import cyr7.visitor.MyIRVisitor;

/**
 * Called by the OneOfThree<IRExpr, IRStmt, IRFuncDecl>'s OneOfThree<IRExpr,
 * IRStmt>.accept(IRVisitor) method. The returned value is relative to the
 * callee node. In this implementation for constant folding, the return type is
 * an OneOfThree<IRExpr, IRStmt, IRFuncDecl>, in which the returned
 * OneOfThree<IRExpr, IRStmt, IRFuncDecl> represents a constant-folded version
 * of that node, if possible.
 * <p>
 * For any node, the fields of nodes are recursively folded. The returned node
 * has all of its child nodes folded if possible.
 *
 * @author ayang
 *
 */
public class DirectConstFoldVisitor
        implements MyIRVisitor<OneOfThree<IRExpr, IRStmt, IRFuncDecl>> {

    // Expressions

    /**
     *
     * If one of the binary operator's two operands is not a constant, then the
     * binary operator node is not a constant.
     * <p>
     * If binary operator's type is MOD or DIV, then if the right operand is is
     * zero, then return.
     *
     */
    @Override
    public OneOfThree<IRExpr, IRStmt, IRFuncDecl> visit(IRBinOp n) {
        IRNodeFactory make = new IRNodeFactory_c(n.location());
        IRExpr leftFold = n.left().accept(this).assertFirst();
        IRExpr rightFold = n.right().accept(this).assertFirst();
        n = make.IRBinOp(n.opType(), leftFold, rightFold);
        if ((n.opType() == OpType.DIV || n.opType() == OpType.MOD)
                && rightFold.isConstant() && rightFold.constant() == 0) {
            return OneOfThree.ofFirst(n);
        }
        return OneOfThree.ofFirst(BinopCalc.calc(n));
    }

    @Override
    public OneOfThree<IRExpr, IRStmt, IRFuncDecl> visit(IRCall n) {
        IRNodeFactory make = new IRNodeFactory_c(n.location());
        List<IRExpr> foldedArgs = n.args().stream().map(a -> {
            return a.accept(this).assertFirst();
        }).collect(Collectors.toList());

        IRExpr target = n.target().accept(this).assertFirst();

        return OneOfThree.ofFirst(make.IRCall(target, foldedArgs));
    }

    /**
     * CFold[Const(n)] ::= Const(n)
     * <ul>
     * <li>No change
     * </ul>
     */
    @Override
    public OneOfThree<IRExpr, IRStmt, IRFuncDecl> visit(IRConst n) {
        return OneOfThree.ofFirst(n);
    }

    /**
     * CFold[ESeq(...s, e)] ::= ESeq(CFold[...s], CFold[e])
     */
    @Override
    public OneOfThree<IRExpr, IRStmt, IRFuncDecl> visit(IRESeq n) {
        IRNodeFactory make = new IRNodeFactory_c(n.location());
        IRStmt stmt = n.stmt().accept(this).assertSecond();
        IRExpr expr = n.expr().accept(this).assertFirst();
        return OneOfThree.ofFirst(make.IRESeq(stmt, expr));
    }

    /**
     * CFold[Mem(e)] ::= Mem(CFold[e])
     *
     */
    @Override
    public OneOfThree<IRExpr, IRStmt, IRFuncDecl> visit(IRMem n) {
        IRNodeFactory make = new IRNodeFactory_c(n.location());
        return OneOfThree
                .ofFirst(make.IRMem(n.expr().accept(this).assertFirst()));
    }

    /**
     * CFold[Name(l)] ::= Name(l)
     * <ul>
     * <li>No change
     * </ul>
     */
    @Override
    public OneOfThree<IRExpr, IRStmt, IRFuncDecl> visit(IRName n) {
        return OneOfThree.ofFirst(n);
    }

    /**
     * CFold[Temp(t)] ::= Temp(t)
     * <ul>
     * <li>No change
     * </ul>
     */
    @Override
    public OneOfThree<IRExpr, IRStmt, IRFuncDecl> visit(IRTemp n) {
        return OneOfThree.ofFirst(n);
    }

    // Statements

    /**
     * CFold[IRCallStmt(etarget, ...argsj)] ::= CallStmt(CFold[etarget],
     * CFold[...argsj])
     */
    @Override
    public OneOfThree<IRExpr, IRStmt, IRFuncDecl> visit(IRCallStmt n) {
        IRNodeFactory make = new IRNodeFactory_c(n.location());

        IRExpr target = n.target().accept(this).assertFirst();
        List<IRExpr> foldedArgs = n.args().stream().map(a -> {
            return a.accept(this).assertFirst();
        }).collect(Collectors.toList());

        return OneOfThree
                .ofSecond(make.IRCallStmt(n.collectors(), target, foldedArgs));
    }

    /**
     * CFold[CJump(etarget, lt, lf)] ::= CJump(CFold[etarget], lt, lf)
     */
    @Override
    public OneOfThree<IRExpr, IRStmt, IRFuncDecl> visit(IRCJump n) {
        IRNodeFactory make = new IRNodeFactory_c(n.location());
        IRExpr cond = n.cond().accept(this).assertFirst();
        return OneOfThree
                .ofSecond(make.IRCJump(cond, n.trueLabel(), n.falseLabel()));
    }

    @Override
    public OneOfThree<IRExpr, IRStmt, IRFuncDecl> visit(IRCompUnit n) {
        IRNodeFactory make = new IRNodeFactory_c(n.location());
        Map<String, IRFuncDecl> functions = new HashMap<>();
        n.functions().forEach((k, v) -> {
            IRFuncDecl f = v.accept(this).assertThird();
            functions.put(k, f);
        });
        return OneOfThree.ofSecond(make.IRCompUnit(n.name(), functions));
    }

    /**
     * CFold[Exp(e)] ::= Exp(CFold[etarget])
     */
    @Override
    public OneOfThree<IRExpr, IRStmt, IRFuncDecl> visit(IRExp n) {
        IRNodeFactory make = new IRNodeFactory_c(n.location());
        IRExpr expr = n.expr().accept(this).assertFirst();
        return OneOfThree.ofSecond(make.IRExp(expr));
    }

    /**
     * CFold[FuncDecl(name, body)] ::= FuncDecl(name, CFold[body])
     */
    @Override
    public OneOfThree<IRExpr, IRStmt, IRFuncDecl> visit(IRFuncDecl n) {
        IRNodeFactory make = new IRNodeFactory_c(n.location());
        IRStmt body = n.body().accept(this).assertSecond();
        return OneOfThree.ofThird(make.IRFuncDecl(n.name(), body));
    }

    /**
     * CFold[Jump(etarget)] ::= Jump(CFold[etarget])
     */
    @Override
    public OneOfThree<IRExpr, IRStmt, IRFuncDecl> visit(IRJump n) {
        IRNodeFactory make = new IRNodeFactory_c(n.location());
        IRExpr target = n.target().accept(this).assertFirst();
        return OneOfThree.ofSecond(make.IRJump(target));
    }

    /**
     * CFold[Label(l)] ::= Label(l)
     * <ul>
     * <li>No Change
     * </ul>
     */
    @Override
    public OneOfThree<IRExpr, IRStmt, IRFuncDecl> visit(IRLabel n) {
        return OneOfThree.ofSecond(n);
    }

    /**
     * CFold[Move(edest, esrc)] ::= Move(CFold[edest], CFold[esrc])
     */
    @Override
    public OneOfThree<IRExpr, IRStmt, IRFuncDecl> visit(IRMove n) {
        IRNodeFactory make = new IRNodeFactory_c(n.location());
        IRExpr src = n.source().accept(this).assertFirst();
        IRExpr dest = n.target().accept(this).assertFirst();
        return OneOfThree.ofSecond(make.IRMove(dest, src));
    }

    /**
     * CFold[Return] ::= Return
     * <ul>
     * <li>No Change
     * </ul>
     */
    @Override
    public OneOfThree<IRExpr, IRStmt, IRFuncDecl> visit(IRReturn n) {
        return OneOfThree.ofSecond(n);
    }

    /**
     * CFold[Seq(...s)] ::= Seq(CFold[...s])
     */
    @Override
    public OneOfThree<IRExpr, IRStmt, IRFuncDecl> visit(IRSeq n) {
        IRNodeFactory make = new IRNodeFactory_c(n.location());
        List<IRStmt> foldedStmts = n.stmts().stream()
                .map(s -> s.accept(this).assertSecond())
                .collect(Collectors.toList());
        return OneOfThree.ofSecond(make.IRSeq(foldedStmts));
    }

}
