package cyr7.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.function.Consumer;

public class CLI {

    final static String usage = "xic [options] <source files>";
    final static int consoleWidth = HelpFormatter.DEFAULT_WIDTH;
    final static int leftPadding = HelpFormatter.DEFAULT_LEFT_PAD;
    final static PrintWriter writer = new PrintWriter(System.out);
    final static HelpFormatter helpFormatter = new HelpFormatter();
    final static Options options = createOptions();
    final static CommandLineParser parser = new DefaultParser();

    static File pathDestination = new File(".");
    static boolean wantsLexing = false;

    /**
     * Creates an {@code Options} instance of the CLI parser. <\br> In this
     * instance, an {@code Option} instance is created for at least each of the
     * following CLI options: {@literal --help}, {@literal --lex}, and
     * {@literal -D <path>}.
     * 
     */
    public static Options createOptions() {
	Options options = new Options();

	Option help = Option.builder("h").longOpt("help")
		.desc("Print a synopsis of options").hasArg(false).argName(null)
		.numberOfArgs(0).required(false).build();

	Option lex = Option.builder("l").longOpt("lex")
		.desc("Generate output from lexical analysis").hasArg(false)
		.argName(null).numberOfArgs(0).required(false).build();

	Option destination = Option.builder("D").longOpt(null)
		.desc("Specify where to place generated diagnostic files")
		.hasArg(true).argName("path").numberOfArgs(1).required(false)
		.build();

	Option version = Option.builder("v").longOpt("version")
		.desc("Version information").hasArg(false).argName(null)
		.numberOfArgs(0).required(false).build();

	return options.addOption(help).addOption(lex).addOption(destination)
		.addOption(version);
    }

    /**
     * Prints a synopsis of the options.
     */
    public static void printHelpMessage() {
	helpFormatter.printHelp(writer, consoleWidth, usage,
		"where possible options include:", options, 0, leftPadding,
		"\n");
	writer.flush();
    }

    /**
     * Prints the version of xic.
     */
    public static void printVersionMessage() {
	writer.println("xic 1.0");
	writer.flush();
    }

    /**
     * Calls the xi lexer to lex the contents of {@code input} and writes the output into
     * {@code output}.
     */
    public static void useLexer(BufferedInputStream input,
	    BufferedOutputStream output) {
	try {
	    output.write(input.readAllBytes());
	    output.flush();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    /**
     * Removes the extension of a filename if an extension exists.
     * @param file A filename
     * @return The filename without the extension.
     */
    public static String removeExtension(File file) {
	String name = file.getName();
	int pos = name.lastIndexOf(".");
	if (pos > 0) {
	    name = name.substring(0, pos);
	}
	return name;
    }

    public static void main(String[] args) {

	// If no arguments or options given, print help.
	if (args.length == 0) {
	    printHelpMessage();
	    return;
	}

	CommandLine cmd;
	try {
	    cmd = parser.parse(options, args);
	} catch (ParseException e) {
	    System.out.println(e.getMessage());
	    return;
	}

	// For each option given, perform task corresponding to option.
	cmd.iterator().forEachRemaining(new Consumer<Option>() {
	    @Override
	    public void accept(Option t) {
		String opt = t.getOpt();
		switch (opt) {
		case "h":
		    printHelpMessage();
		    break;
		case "l": {
		    wantsLexing = true;
		    break;
		}
		case "D":
		    String directory = cmd.getOptionValue("D");
		    pathDestination = new File(directory);
		    break;
		case "v":
		    printVersionMessage();
		    break;
		default:
		    System.out.println("No case for given for option: " + opt);
		    break;
		}
	    }
	});

	if (wantsLexing) {
	    String[] sourceFiles = cmd.getArgs();
	    for (String filename : sourceFiles) {
		File file = new File(filename);
		BufferedInputStream inputStream;
		BufferedOutputStream outputStream;
		try {
		    inputStream = new BufferedInputStream(
			    new FileInputStream(file));
		    File destination = new File(
			    pathDestination.getAbsolutePath(),
			    removeExtension(file) + ".lexed");
		    outputStream = new BufferedOutputStream(
			    new FileOutputStream(destination));
		    useLexer(inputStream, outputStream);
		} catch (FileNotFoundException e) {
		    writer.write(e.getMessage());
		    writer.flush();
		}
	    }
	}
	writer.close();
    }
}
