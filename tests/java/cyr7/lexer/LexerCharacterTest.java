package cyr7.lexer;

import cyr7.exceptions.InvalidCharacterLiteralException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LexerCharacterTest {

    @Test
    void characterEscapingUnicode() throws IOException {
        MyLexer lexer = new MyLexer(new StringReader("'\\xAAAA'")); // \xAAAA
        MyLexer.Token token = lexer.nextToken();
        assertEquals(MyLexer.TokenType.CHAR_LITERAL, token.type);
        assertEquals("\uAAAA", token.attribute);

        lexer = new MyLexer(new StringReader("'\\x123'")); // \x123
        token = lexer.nextToken();
        assertEquals(MyLexer.TokenType.CHAR_LITERAL, token.type);
        assertEquals("\u0123", token.attribute);

        lexer = new MyLexer(new StringReader("'\\xFD'")); // \xFD
        token = lexer.nextToken();
        assertEquals(MyLexer.TokenType.CHAR_LITERAL, token.type);
        assertEquals("\u00FD", token.attribute);

        lexer = new MyLexer(new StringReader("'\\x0'")); // \x0
        token = lexer.nextToken();
        assertEquals(MyLexer.TokenType.CHAR_LITERAL, token.type);
        assertEquals("\u0000", token.attribute);
    }

    @Test
    void characterEscapingDoubleQuote() throws IOException {
        MyLexer lexer = new MyLexer(new StringReader("'\\\"'")); // \"
        MyLexer.Token token = lexer.nextToken();
        assertEquals(MyLexer.TokenType.CHAR_LITERAL, token.type);
        assertEquals("\"", token.attribute);

        lexer = new MyLexer(new StringReader("'\"'")); // \"
        token = lexer.nextToken();
        assertEquals(MyLexer.TokenType.CHAR_LITERAL, token.type);
        assertEquals("\"", token.attribute);
    }

    @Test
    void characterEscapingSingleQuote() throws IOException {
        MyLexer lexer = new MyLexer(new StringReader("'\\''")); // \'
        MyLexer.Token token = lexer.nextToken();
        assertEquals(MyLexer.TokenType.CHAR_LITERAL, token.type);
        assertEquals("'", token.attribute);
    }

    @Test
    void characterEscapingNewLine() throws IOException {
        MyLexer lexer = new MyLexer(new StringReader("'\\n'")); // \n
        MyLexer.Token token = lexer.nextToken();
        assertEquals(MyLexer.TokenType.CHAR_LITERAL, token.type);
        assertEquals("\n", token.attribute);
    }

    @Test
    void characterInvalidEscaping() {
        MyLexer lexer = new MyLexer(new StringReader("'\\x'")); // \x
        assertThrows(Exception.class, lexer::nextToken);
        lexer = new MyLexer(new StringReader("'\\g'")); // \g
        assertThrows(Exception.class, lexer::nextToken);
        lexer = new MyLexer(new StringReader("'\\b'")); // \b
        assertThrows(Exception.class, lexer::nextToken);
    }

    @Test
    void characterEscapingBackslash() throws IOException {
        MyLexer lexer = new MyLexer(new StringReader("'\\\\'")); // \\
        MyLexer.Token token = lexer.nextToken();
        assertEquals(MyLexer.TokenType.CHAR_LITERAL, token.type);
        assertEquals("\\", token.attribute);
    }

    @Test
    void characterInvalidFormat() {
        // empty character literal
        MyLexer lexer = new MyLexer(new StringReader("''"));
        assertThrows(InvalidCharacterLiteralException.class, lexer::nextToken);

        // two line character literal
        lexer = new MyLexer(new StringReader("'as'"));
        assertThrows(InvalidCharacterLiteralException.class, lexer::nextToken);

        // outside of (unicode) BMP
        lexer = new MyLexer(new StringReader("\u583c\u5f46"));
        assertThrows(Exception.class, lexer::nextToken);
    }

    @Test
    void characterTabTest() throws IOException {
        MyLexer lexer = new MyLexer(new StringReader("'\t'"));
        MyLexer.Token token = lexer.nextToken();
        assertEquals(MyLexer.TokenType.CHAR_LITERAL, token.type);
        assertEquals("\t", token.attribute);
    }

}
