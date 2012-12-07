package org.eobjects.datacleaner.groovy;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eobjects.analyzer.beans.api.Categorized;
import org.eobjects.analyzer.beans.api.Close;
import org.eobjects.analyzer.beans.api.Configured;
import org.eobjects.analyzer.beans.api.Description;
import org.eobjects.analyzer.beans.api.Initialize;
import org.eobjects.analyzer.beans.api.OutputColumns;
import org.eobjects.analyzer.beans.api.StringProperty;
import org.eobjects.analyzer.beans.api.Transformer;
import org.eobjects.analyzer.beans.api.TransformerBean;
import org.eobjects.analyzer.beans.categories.ScriptingCategory;
import org.eobjects.analyzer.beans.convert.ConvertToStringTransformer;
import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.data.InputRow;
import org.eobjects.analyzer.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TransformerBean("Groovy transformer")
@Categorized(ScriptingCategory.class)
@Description("Perform almost any kind of data transformation with the use of the Groovy language.")
public class GroovyTransformer implements Transformer<String> {

    private static final Logger logger = LoggerFactory.getLogger(GroovyTransformer.class);

    @Configured(order = 1)
    InputColumn<?>[] inputs;

    @Configured(order = 2)
    @Description("Execute the transformation code in a concurrent manner?")
    boolean concurrent = true;

    @Configured(order = 3)
    @StringProperty(multiline = true, mimeType = { "application/x-groovy", "text/x-groovy", "text/groovy" })
    String code = "class Transformer {\n  void initialize() {\n    //optional initializer\n  }\n\n  String transform(map) {\n    return map.toString()\n  }\n}";;

    private GroovyObject _groovyObject;
    private GroovyClassLoader _groovyClassLoader;

    @Initialize
    public void init() {
        ClassLoader parent = getClass().getClassLoader();
        _groovyClassLoader = new GroovyClassLoader(parent);
        logger.debug("Compiling Groovy code:\n{}", code);
        final Class<?> groovyClass = _groovyClassLoader.parseClass(code);
        _groovyObject = (GroovyObject) ReflectionUtils.newInstance(groovyClass);
        _groovyObject.invokeMethod("initialize", new Object[] {});
    }

    @Close
    public void close() {
        _groovyObject = null;
        _groovyClassLoader.clearCache();
        _groovyClassLoader = null;
    }

    public OutputColumns getOutputColumns() {
        return new OutputColumns("Groovy output");
    }

    public String[] transform(InputRow inputRow) {
        final Map<String, Object> map = new LinkedHashMap<String, Object>();
        for (InputColumn<?> input : inputs) {
            map.put(input.getName(), inputRow.getValue(input));
        }
        final Object[] args = new Object[] { map };
        final Object result;
        if (concurrent) {
            result = _groovyObject.invokeMethod("transform", args);
        } else {
            synchronized (_groovyObject) {
                result = _groovyObject.invokeMethod("transform", args);
            }
        }
        logger.debug("Transformation result: {}", result);
        final String stringResult = ConvertToStringTransformer.transformValue(result);
        return new String[] { stringResult };
    }

}
