package cyr7.integration;

import org.junit.jupiter.api.Tag;

@Tag("core")
public class TestCTranslation extends TestProgram {
    @Override
    protected String filename() {
        return "ctranslation";
    }

    @Override
    protected String expected() {
        return "1\n0\n0\n5\n50\n";
    }
}
