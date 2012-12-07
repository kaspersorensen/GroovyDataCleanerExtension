package org.eobjects.datacleaner.groovy;

import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.data.MockInputColumn;
import org.eobjects.analyzer.data.MockInputRow;

import junit.framework.TestCase;

public class GroovyTransformerTest extends TestCase {

    public void testScenario() throws Exception {
        GroovyTransformer transformer = new GroovyTransformer();

        InputColumn<String> col1 = new MockInputColumn<String>("foo");
        InputColumn<String> col2 = new MockInputColumn<String>("bar");

        transformer.inputs = new InputColumn[] { col1, col2 };
        transformer.code = "class Transformer {\n" + "void initialize(){println(\"hello\")}\n"
                + "String transform(map){println(map); return \"foo\"}\n" + "}";

        transformer.init();

        String[] result = transformer.transform(new MockInputRow().put(col1, "Kasper").put(col2, "Sørensen"));
        assertEquals(1, result.length);
        assertEquals("foo", result[0]);
        
        transformer.close();
    }
}
