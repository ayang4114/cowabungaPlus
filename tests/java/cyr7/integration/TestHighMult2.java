package cyr7.integration;

import org.junit.jupiter.api.Tag;

@Tag("core")
public class TestHighMult2 extends TestProgram {
    @Override
    protected String filename() {
        return "highMult2";
    }

    @Override
    protected String expected() {
        return "-1\n";
    }
}
