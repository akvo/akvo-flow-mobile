package org.akvo.flow.serialization.response.value;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class OptionValueTest extends TestCase {

    public void testSingle() {
        List<Option> options = new ArrayList<>();
        Option option = new Option();
        option.setText("United Kingdom");
        option.setCode("UK");
        options.add(option);

        String response = OptionValue.serialize(options);
        List<Option> values = OptionValue.deserialize(response);

        assertEquals(1, values.size());
        assertEquals("United Kingdom", values.get(0).getText());
        assertEquals("UK", values.get(0).getCode());
    }

    public void testSingleOther() {
        List<Option> options = new ArrayList<>();
        Option option = new Option();
        option.setText("United Kingdom");
        option.setCode("OTHER");
        option.setIsOther(true);
        options.add(option);

        String response = OptionValue.serialize(options);
        List<Option> values = OptionValue.deserialize(response);

        assertEquals(1, values.size());
        assertEquals("United Kingdom", values.get(0).getText());
        assertEquals("OTHER", values.get(0).getCode());
        assertTrue(values.get(0).isOther());
    }

    public void testSingleNoCodes() {
        List<Option> options = new ArrayList<>();
        Option option = new Option();
        option.setText("United Kingdom");
        options.add(option);

        String response = OptionValue.serialize(options);
        List<Option> values = OptionValue.deserialize(response);

        assertEquals(1, values.size());
        assertEquals("United Kingdom", values.get(0).getText());
        assertNull(values.get(0).getCode());
    }

    public void testMultiple() {
        List<Option> options = new ArrayList<>();
        Option option = new Option();
        option.setText("United Kingdom");
        option.setCode("UK");
        options.add(option);

        option = new Option();
        option.setText("Spain");
        option.setCode("ES");
        options.add(option);

        String response = OptionValue.serialize(options);

        List<Option> values = OptionValue.deserialize(response);

        assertEquals(2, values.size());
        assertEquals("United Kingdom", values.get(0).getText());
        assertEquals("UK", values.get(0).getCode());
        assertEquals("Spain", values.get(1).getText());
        assertEquals("ES", values.get(1).getCode());
    }

    public void testDependencies() {
        List<Option> options = new ArrayList<>();
        Option option = new Option();
        option.setText("United Kingdom");
        option.setCode("UK");
        options.add(option);

        option = new Option();
        option.setText("Spain");
        option.setCode("ES");
        options.add(option);

        String response = OptionValue.serialize(options);

        Dependency dependency = new Dependency();
        dependency.setAnswer("Spain");
        assertTrue(dependency.isMatch(response));

        dependency.setAnswer("Spain|Finland");
        assertTrue(dependency.isMatch(response));

        dependency.setAnswer("Spain|United Kingdom");
        assertTrue(dependency.isMatch(response));

        dependency.setAnswer("Other");
        assertFalse(dependency.isMatch(response));
    }
}
