package cyr7.semantics;

import cyr7.exceptions.UnbalancedPushPopException;
import cyr7.semantics.context.Context;
import cyr7.semantics.types.ArrayType;
import cyr7.semantics.types.OrdinaryType;
import cyr7.semantics.types.PrimitiveType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public abstract class TestContext {

    private final Optional<OrdinaryType> INT =
                                    Optional.of(PrimitiveType.intDefault);
    private final Optional<OrdinaryType> BOOL = 
                                    Optional.of(PrimitiveType.boolDefault);
    private final Optional<OrdinaryType> INT_ARRAY = Optional.of(
                                        new ArrayType(PrimitiveType.intDefault));
    private final Optional<OrdinaryType> BOOL_ARRAY = Optional.of(
                                        new ArrayType(PrimitiveType.boolDefault));

    abstract Context createEmptyContext();

    @Test
    void testUnbalancedPopThrowsException() throws UnbalancedPushPopException {
        assertThrows(UnbalancedPushPopException.class, () ->
            createEmptyContext().pop()
        );

        // popping from context twice should throw the exception both times
        Context context = createEmptyContext();
        context.push().pop();
        assertThrows(UnbalancedPushPopException.class, context::pop);
        assertThrows(UnbalancedPushPopException.class, context::pop);

        context = createEmptyContext();
        context.push().push().pop().push().pop().pop();
        assertThrows(UnbalancedPushPopException.class, context::pop);
    }

    @Test
    void testAddGetInteraction() {
        Context context = createEmptyContext();
        context.addVar("var1", INT.get());
        context.addVar("var2", BOOL.get());
        context.addVar("var3", INT_ARRAY.get());

        assertEquals(INT, context.getVar("var1"));
        assertEquals(BOOL, context.getVar("var2"));
        assertEquals(INT_ARRAY, context.getVar("var3"));

        assertEquals(INT_ARRAY, context.getVar("var3"));
        assertEquals(INT, context.getVar("var1"));
        assertEquals(BOOL, context.getVar("var2"));

        assertTrue(context.getVar("var4").isEmpty());
        context.addVar("var4", BOOL_ARRAY.get());
        assertEquals(BOOL_ARRAY, context.getVar("var4"));

        assertEquals(INT, context.getVar("var1"));
        assertEquals(BOOL, context.getVar("var2"));
        assertEquals(INT_ARRAY, context.getVar("var3"));

        assertTrue(context.getVar("whatever").isEmpty());
    }

    @Test
    void testContext1() throws UnbalancedPushPopException {
        Context context = createEmptyContext();

        context.addVar("level1", INT.get());
        assertEquals(INT, context.getVar("level1"));
        assertTrue(context.getVar("level2").isEmpty());
        assertTrue(context.getVar("level3").isEmpty());

        context.push();
        assertEquals(INT, context.getVar("level1"));
        assertTrue(context.getVar("level2").isEmpty());
        assertTrue(context.getVar("level3").isEmpty());

        context.addVar("level2", BOOL.get());
        assertEquals(INT, context.getVar("level1"));
        assertEquals(BOOL, context.getVar("level2"));
        assertTrue(context.getVar("level3").isEmpty());

        context.push();
        assertEquals(INT, context.getVar("level1"));
        assertEquals(BOOL, context.getVar("level2"));
        assertTrue(context.getVar("level3").isEmpty());

        context.addVar("level3", INT_ARRAY.get());
        assertEquals(INT, context.getVar("level1"));
        assertEquals(BOOL, context.getVar("level2"));
        assertEquals(INT_ARRAY, context.getVar("level3"));

        context.pop();
        assertEquals(INT, context.getVar("level1"));
        assertEquals(BOOL, context.getVar("level2"));
        assertTrue(context.getVar("level3").isEmpty());

        context.pop();
        assertEquals(INT, context.getVar("level1"));
        assertTrue(context.getVar("level2").isEmpty());
        assertTrue(context.getVar("level3").isEmpty());

        assertThrows(UnbalancedPushPopException.class, context::pop);
    }

}
