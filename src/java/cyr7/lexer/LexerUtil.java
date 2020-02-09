package cyr7.lexer;

import cyr7.exceptions.LexerException;
import cyr7.parser.sym;
import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.ComplexSymbolFactory.ComplexSymbol;
import java_cup.runtime.Symbol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class LexerUtil {

	/**
	 * Read contents and write a line by line description of the lexed output.
	 *
	 * @param reader the contents to read
	 * @param writer the place to write
	 * @throws IOException if the reader throws an {@code IOException}
	 */
	public static void lex(Reader reader, Writer writer) throws IOException {
		MyLexer lexer = new MyLexer(new BufferedReader(reader), new ComplexSymbolFactory());
		Symbol token;

		try {
			while ((token = lexer.next_token()).sym != sym.EOF) {
				writer.append(fullDescription(token))
						.append(System.lineSeparator());
			}
		} catch (Exception e) {
			writer.append(e.getMessage()).append(System.lineSeparator());
		}
		writer.flush();
	}

	/**
	 * A description of a token including line and column number in the format:
	 *
	 * {@code $line:$column $desc}
	 *
	 * @param token the symbol to describe
	 * @return a description of the token
	 */
	static String fullDescription(Symbol token) {
		int line = ((ComplexSymbol) token).xleft.getLine();
		int column = ((ComplexSymbol) token).xleft.getColumn();

		String location = "" + line + ":" + column + " ";

		switch (token.sym) {
			case sym.USE:
				return location + "use";
			case sym.IF:
				return location + "if";
			case sym.WHILE:
				return location + "while";
			case sym.ELSE:
				return location + "else";
			case sym.RETURN:
				return location + "return";
			case sym.LENGTH:
				return location + "length";

			case sym.TYPE_INT:
				return location + "int";
			case sym.TYPE_BOOL:
				return location + "bool";

			case sym.BOOL_LITERAL:
				return location + token.value;
			case sym.INT_LITERAL:
				return location + "integer " + token.value;
			case sym.CHAR_LITERAL:
				return location + "character " + unescapeString((String) token.value);
			case sym.STRING_LITERAL:
				return location + "string " + unescapeString((String) token.value);

			case sym.ID:
				return location + "id " + token.value;

			case sym.L_PAREN:
				return location + "(";
			case sym.R_PAREN:
				return location + ")";
			case sym.L_SQ_BRKT:
				return location + "[";
			case sym.R_SQ_BRKT:
				return location + "]";
			case sym.L_BRACE:
				return location + "{";
			case sym.R_BRACE:
				return location + "}";
			case sym.COLON:
				return location + ":";
			case sym.SEMICOLON:
				return location + ";";
			case sym.COMMA:
				return location + ",";
			case sym.UNDERSCORE:
				return location + "_";

			case sym.ASSIGN:
				return location + "=";

			case sym.PLUS:
				return location + "+";
			case sym.MINUS:
				return location + "-";
			case sym.MULT:
				return location + "*";
			case sym.HIGH_MULT:
				return location + "*>>";
			case sym.DIVIDE:
				return location + "/";
			case sym.REMAINDER:
				return location + "%";

			case sym.NEG_BOOL:
				return location + "!";

			case sym.LT:
				return location + "<";
			case sym.LTE:
				return location + "<=";
			case sym.GT:
				return location + ">";
			case sym.GTE:
				return location + ">=";

			case sym.EQUALS:
				return location + "==";
			case sym.NOT_EQUALS:
				return location + "!=";

			case sym.LOGICAL_AND:
				return location + "&";
			case sym.LOGICAL_OR:
				return location + "|";

			default:
				throw new RuntimeException(
						"Token " + token.sym + " is missing a description.");
		}

	}

	/**
	 * Replace whitespace characters that were escaped by the lexer back into
	 * their escaped form.
	 *
	 * For example: {@code unescapeString("\t")`} is a literal backslash and character
	 * 't'
	 *
	 * @param s The string to unescape
	 * @return The escaped string
	 */
	public static String unescapeString(String s){
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++)
			switch (s.charAt(i)){
				case '\n': sb.append("\\n"); break;
				case '\t': sb.append("\\t"); break;
				case '\r': sb.append("\\r"); break;
				case '\f': sb.append("\\f"); break;
				default: sb.append(s.charAt(i));
			}
		return sb.toString();
	}

    private LexerUtil() {
    }

}
