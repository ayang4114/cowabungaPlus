package cyr7.cfg;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;

import cyr7.cli.OptConfig;
import cyr7.cli.Optimization;
import org.junit.jupiter.api.Test;

import cyr7.cfg.ir.CFGUtil;
import cyr7.ir.IRUtil;
import cyr7.typecheck.IxiFileOpener;

class TestDotGeneration {

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



    @Test
    void test() throws IOException {
        final String prgmString = "main() { i: int = 232 "
                + "while (false) {"
                + "    if (i%2 == 0) { i = 32 } else {"
                + "        k: int = 0"
                + "        while (k < 12) k = k + 1"
                + "    }"
                + "    i = i + 1"
                + "}"
                + "}";


        Reader r = new StringReader(prgmString);
        FileWriter w = new FileWriter("output.dot");
        OptConfig config = OptConfig.of(Optimization.CF);
        try {
            IRUtil.irGen(r, new PrintWriter(System.out), "main", new Opener(), config);
            CFGUtil.generateDot(r, w, "main", new Opener(), config, "_Imain_p");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
