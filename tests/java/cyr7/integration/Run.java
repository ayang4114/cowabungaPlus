package cyr7.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import cyr7.C;
import cyr7.ast.ASTFactory;
import cyr7.ast.expr.ExprNode;
import cyr7.ast.toplevel.FunctionDeclNode;
import cyr7.ast.toplevel.XiProgramNode;
import cyr7.cli.OptConfig;
import cyr7.ir.DefaultIdGenerator;
import cyr7.ir.IRUtil;
import cyr7.ir.IdGenerator;
import cyr7.ir.interpret.IRSimulator;
import cyr7.ir.nodes.IRCompUnit;
import cyr7.ir.nodes.IRNode;
import cyr7.ir.visit.CheckCanonicalIRVisitor;
import cyr7.ir.visit.CheckConstFoldedIRVisitor;
import cyr7.parser.ParserUtil;
import cyr7.typecheck.IxiFileOpener;
import cyr7.typecheck.TypeCheckUtil;
import cyr7.visitor.VisitorFactory;
import edu.cornell.cs.cs4120.util.CodeWriterSExpPrinter;
import edu.cornell.cs.cs4120.util.SExpPrinter;
import java_cup.runtime.ComplexSymbolFactory.Location;

public final class Run {

    private static final ASTFactory ast = new ASTFactory(C.LOC);
    public static final class RunConfiguration {

        public final String[] args;

        public final boolean bigHeap;

        public final String stdin;
        public RunConfiguration() {
            this.args = new String[] { };
            this.bigHeap = false;
            this.stdin = "";
        }

        public RunConfiguration(String[] args, boolean bigHeap, String stdin) {
            this.args = args;
            this.bigHeap = bigHeap;
            this.stdin = stdin;
        }

        public RunConfiguration args(String... args) {
            return new RunConfiguration(args, this.bigHeap, this.stdin);
        }

        public RunConfiguration bigHeap(boolean bigHeap) {
            return new RunConfiguration(this.args, bigHeap, this.stdin);
        }

        public RunConfiguration stdin(String stdin) {
            return new RunConfiguration(this.args, this.bigHeap, stdin);
        }

    }

    private static class Opener implements IxiFileOpener {

        @Override
        public Reader openIxiLibraryFile(String name) throws FileNotFoundException {
            if (name.equals("conv")) {
                return new StringReader(
                    "parseInt(str: int[]): int, bool\n"
                    + "unparseInt(n: int): int[]\n"
                );
            } else if (name.equals("io")) {
                return new StringReader(
                    "print(str: int[])\n"
                    + "println(str: int[])\n"
                    + "readln() : int[]\n"
                    + "getchar() : int\n"
                    + "eof() : bool\n"
                );
            } else {
                throw new FileNotFoundException(name);
            }
        }

    }

    public static String getFile(String filename) throws Exception {
        InputStream filePath = Run.class
            .getClassLoader()
            .getResourceAsStream("integration/" + filename + ".xi");
        assert filePath != null;
        return new String(filePath.readAllBytes());
    }

    private static IRCompUnit lower(
        IRCompUnit compUnit,
        IdGenerator generator,
        OptConfig optConfig) {

        IRCompUnit lowered = IRUtil.lower(compUnit, generator, optConfig);

        if (optConfig.cf()) {
            assertTrue(lowered.aggregateChildren(new CheckConstFoldedIRVisitor()));
        }

        CheckCanonicalIRVisitor visitor = new CheckCanonicalIRVisitor();
        boolean checkLowered = lowered.aggregateChildren(visitor);
        if (!checkLowered) {
            fail("Program is not lowered, but it's supposed to be!: "
                + sexp(lowered)
                + "\nOffending node: "
                + visitor.noncanonical());
        }

        return lowered;
    }

    private static XiProgramNode addPremain(XiProgramNode toModify, String[] args) {
        List<ExprNode> exprArgs = new ArrayList<>();
        exprArgs.add(ast.string("asdlkfjasldfkjdbkljad"));
        for (String arg : args) {
            exprArgs.add(ast.string(arg));
        }

        FunctionDeclNode premain =
                ast.funcDecl(ast.funcHeader("*premain*", List.of(), List.of()), ast.block(
                        ast.procedure(ast.call("main", ast.array(exprArgs)))
                ));
        List<FunctionDeclNode> functionDecls = new ArrayList<>(toModify.functions);
        functionDecls.add(premain);

        return new XiProgramNode(toModify.getLocation(), toModify.uses, functionDecls);
    }

    public static String mirRun(String program, RunConfiguration runConfiguration) throws Exception {
        Reader reader = new StringReader(program);

        XiProgramNode result = (XiProgramNode) ParserUtil.parseNode(reader, "Run", false);
        result = addPremain(result, runConfiguration.args);
        TypeCheckUtil.typeCheck(result, new Opener());

        IdGenerator generator = new DefaultIdGenerator();

        IRCompUnit compUnit;
        {
            IRNode node = result.accept(VisitorFactory.Companion.astToIrVisitor(generator)).assertSecond();
            assert node instanceof IRCompUnit;
            compUnit = (IRCompUnit) node;
        }

        InputStream systemIn = System.in;
        InputStream inputStream = new ByteArrayInputStream(runConfiguration.stdin.getBytes());
        System.setIn(inputStream);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IRSimulator sim = new IRSimulator(
            compUnit,
            runConfiguration.bigHeap ? IRSimulator.getBIG_HEAP_SIZE() : IRSimulator.getDEFAULT_HEAP_SIZE(),
            new PrintStream(outputStream)
        );
        System.setIn(systemIn);
        sim.call("_I*premain*_p", 0);
        return new String(outputStream.toByteArray(), Charset.defaultCharset());
    }

    public static String lirRun(String program, OptConfig optConfig, RunConfiguration runConfiguration) throws Exception {
        Reader reader = new StringReader(program);

        XiProgramNode result = (XiProgramNode) ParserUtil.parseNode(reader, "Run", false);
        result = addPremain(result, runConfiguration.args);
        TypeCheckUtil.typeCheck(result, new Opener());

        IdGenerator generator = new DefaultIdGenerator();

        IRCompUnit compUnit;
        {
            IRNode node = result.accept(VisitorFactory.Companion.astToIrVisitor(generator)).assertSecond();
            assert node instanceof IRCompUnit;
            compUnit = (IRCompUnit) node;
        }

        IRCompUnit lowered = lower(compUnit, generator, optConfig);

        InputStream systemIn = System.in;
        InputStream inputStream = new ByteArrayInputStream(runConfiguration.stdin.getBytes());
        System.setIn(inputStream);


        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IRSimulator sim = new IRSimulator(
            lowered,
            runConfiguration.bigHeap ? IRSimulator.Companion.getBIG_HEAP_SIZE() : IRSimulator.Companion.getDEFAULT_HEAP_SIZE(),
            new PrintStream(outputStream)
        );
        System.setIn(systemIn);

        sim.call("_I*premain*_p", 0);
        return new String(outputStream.toByteArray(), Charset.defaultCharset());
    }

    private static String sexp(IRNode n) {
        StringWriter writer = new StringWriter();
        SExpPrinter printer =
            new CodeWriterSExpPrinter(new PrintWriter(writer));
        n.printSExp(printer);
        printer.flush();
        return writer.toString();
    }

    private Run() { }

}
