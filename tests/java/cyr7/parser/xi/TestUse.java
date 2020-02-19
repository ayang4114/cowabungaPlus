package cyr7.parser.xi;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedList;

import org.junit.jupiter.api.Test;

import cyr7.ast.FunctionHeaderDeclNode;
import cyr7.ast.UseNode;
import cyr7.ast.XiProgramNode;
import cyr7.ast.stmt.VarDeclStmtNode;
import cyr7.ast.type.TypeExprNode;
import cyr7.parser.XiParser;
import cyr7.parser.util.ParserFactory;

class TestUse {

    LinkedList<VarDeclStmtNode> args;
    LinkedList<TypeExprNode> returnTypes;
    FunctionHeaderDeclNode function;
    LinkedList<FunctionHeaderDeclNode> functions;
    XiProgramNode expected;
    StringReader prgm;
    XiParser parser;
    Object tree;

    @Test
    void test() throws Exception {
        String program = "use io\nuse math;\nuse nothing";
        LinkedList<UseNode> uses = new LinkedList<>();
        Collections.addAll(
            uses,
            new UseNode(null, "io"),
            new UseNode(null, "math"),
            new UseNode(null, "nothing")
        );
        expected = new XiProgramNode(null, uses, new LinkedList<>());
        parser = ParserFactory.make(program, false);
        tree = parser.parse().value;
        assertEquals(expected, tree);
    }

}
