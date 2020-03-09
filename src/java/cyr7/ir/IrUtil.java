package cyr7.ir;

import cyr7.C;
import cyr7.ast.Node;
import cyr7.ir.interpret.IRSimulator;
import cyr7.ir.lowering.LoweringVisitor;
import cyr7.ir.lowering.LoweringVisitor.Result;
import cyr7.ir.nodes.IRCompUnit;
import cyr7.ir.nodes.IRNode;
import cyr7.ir.nodes.IRNodeFactory;
import cyr7.ir.nodes.IRNodeFactory_c;
import cyr7.ir.nodes.IRSeq;
import cyr7.parser.ParserUtil;
import cyr7.typecheck.TypeCheckUtil;
import edu.cornell.cs.cs4120.util.CodeWriterSExpPrinter;
import edu.cornell.cs.cs4120.util.SExpPrinter;
import java_cup.runtime.ComplexSymbolFactory.Location;

import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;

public class IrUtil {
    
    public static void mirRun(Reader reader, Writer writer, String filename,
            boolean isIXI) throws Exception {
        
        Node result = ParserUtil.parseNode(reader, filename, isIXI);
        TypeCheckUtil.typeCheck(result);
        IRCompUnit compUnit = (IRCompUnit) result.accept(new AstToIrVisitor())
                .assertSecond();
        IRSimulator sim = new IRSimulator(compUnit);
        long retVal = sim.call("_Imain_paai", 0);
        writer.append(String.valueOf(retVal)).append(System.lineSeparator());
    }
    
    public static void irGen(Reader reader, Writer writer, String filename,
            boolean isIXI) throws Exception {
        IRNodeFactory make = new IRNodeFactory_c(new Location(1,1));
        Node result = ParserUtil.parseNode(reader, filename, isIXI);
        TypeCheckUtil.typeCheck(result);
        IdGenerator generator = new DefaultIdGenerator();
        IRNode node = result.accept(new AstToIrVisitor(generator))
                .assertSecond();
        IRSeq lowered = make.IRSeq(node.accept(
                new LoweringVisitor(generator)).assertFirst()); 
        
        // TODO: Add optimization conditions
        SExpPrinter printer = new CodeWriterSExpPrinter(new PrintWriter(writer));
        lowered.printSExp(printer);
        printer.flush();
    }
    
    public static void irRun(Reader reader, Writer writer, String filename,
            boolean isIXI) throws Exception {
        Node result = ParserUtil.parseNode(reader, filename, isIXI);
        TypeCheckUtil.typeCheck(result);
        IdGenerator generator = new DefaultIdGenerator();
        IRNode node = result.accept(new AstToIrVisitor(generator))
                .assertSecond();
        IRCompUnit lowered = node.accept(new LoweringVisitor(generator)).assertThird();
        
        // TODO: Add optimization conditions
        IRSimulator sim = new IRSimulator(lowered);
        long retVal = sim.call("_Imain_paai", 0);
        writer.append(String.valueOf(retVal)).append(System.lineSeparator());
    }
    
    
}