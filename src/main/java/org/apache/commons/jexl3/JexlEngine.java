/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.math.MathContext;
import org.apache.commons.jexl3.introspection.JexlUberspect;

/**
 * Creates and evaluates JexlExpression and JexlScript objects.
 * Determines the behavior of expressions & scripts during their evaluation with respect to:
 * <ul>
 *  <li>Introspection, see {@link JexlUberspect}</li>
 *  <li>Arithmetic & comparison, see {@link JexlArithmetic}</li>
 *  <li>Error reporting</li>
 *  <li>Logging</li>
 * </ul>
 * <p>
 * Note that methods that evaluate expressions may throw <em>unchecked</em> exceptions;
 * The {@link JexlException} are thrown in "non-silent" mode but since these are
 * RuntimeException, user-code <em>should</em> catch them wherever most appropriate.
 * </p>
 * @since 2.0
 */
public abstract class JexlEngine {
    /**
     * Script evaluation options.
     * <p>The JexlContext used for evaluation can implement this interface to alter behavior.</p>
     */
    public interface Options {
        /**
         * Sets whether the engine will throw a {@link JexlException} when an error is encountered during evaluation.
         * @return true if silent, false otherwise
         */
        Boolean isSilent();

        /**
         * Checks whether the engine considers unknown variables, methods, functions and constructors as errors or
         * evaluates them as null.
         * @return true if strict, false otherwise
         */
        Boolean isStrict();

        /**
         * Checks whether the arithmetic triggers errors during evaluation when null is used as an operand.
         * @return true if strict, false otherwise
         */
        Boolean isStrictArithmetic();

        /**
         * The MathContext instance used for +,-,/,*,% operations on big decimals.
         * @return the math context
         */
        MathContext getArithmeticMathContext();

        /**
         * The BigDecimal scale used for comparison and coercion operations.
         * @return the scale
         */
        int getArithmeticMathScale();
    }

    /**
     * Gets this engine underlying {@link JexlUberspect}.
     * @return the uberspect
     */
    public abstract JexlUberspect getUberspect();

    /**
     * Gets this engine underlying {@link JexlArithmetic}.
     * @return the arithmetic
     */
    public abstract JexlArithmetic getArithmetic();

    /**
     * Checks whether this engine is in debug mode.
     * @return true if debug is on, false otherwise
     */
    public abstract boolean isDebug();

    /**
     * Checks whether this engine throws JexlException during evaluation.
     * @return true if silent, false (default) otherwise
     */
    public abstract boolean isSilent();

    /**
     * Checks whether the engine considers unknown variables, methods, functions and constructors as errors.
     * @return true if strict, false otherwise
     */
    public abstract boolean isStrict();

    /**
     * Sets the class loader used to discover classes in 'new' expressions.
     * <p>This method is <em>not</em> thread safe; it should be called as an optional step of the JexlEngine
     * initialization code before expression creation &amp; evaluation.</p>
     * @param loader the class loader to use
     */
    public abstract void setClassLoader(ClassLoader loader);

    /**
     * Creates a new {@link JxltEngine} instance using this engine.
     * @return a Jexl Template engine
     */
    public abstract JxltEngine createJxltEngine();

    /**
     * Clears the expression cache.
     */
    public abstract void clearCache();

    /**
     * Creates an JexlExpression from a String containing valid JEXL syntax.
     * This method parses the expression which must contain either a reference or an expression.
     *
     * @param expression A String containing valid JEXL syntax
     * @return An {@link JexlExpression} which can be evaluated using a {@link JexlContext}
     * @throws JexlException An exception can be thrown if there is a problem parsing this expression, or if the
     * expression is neither an expression nor a reference.
     */
    public abstract JexlExpression createExpression(String expression);

    /**
     * Creates an JexlExpression from a String containing valid JEXL syntax.
     * This method parses the expression which  must contain either a reference or an expression.
     *
     * @param expression A String containing valid JEXL syntax
     * @param info An info structure to carry debugging information if needed
     * @return An {@link JexlExpression} which can be evaluated using a {@link JexlContext}
     * @throws JexlException An exception can be thrown if there is a problem parsing this expression, or if the
     * expression is neither an expression or a reference.
     */
    public abstract JexlExpression createExpression(String expression, JexlInfo info);

    /**
     * Creates a Script from a String containing valid JEXL syntax.
     * This method parses the script which validates the syntax.
     *
     * @param scriptText A String containing valid JEXL syntax
     * @return A {@link JexlScript} which can be executed using a {@link JexlContext}
     * @throws JexlException if there is a problem parsing the script.
     */
    public abstract JexlScript createScript(String scriptText);

    /**
     * Creates a Script from a String containing valid JEXL syntax.
     * This method parses the script which validates the syntax.
     *
     * @param scriptText A String containing valid JEXL syntax
     * @param names the script parameter names
     * @return A {@link JexlScript} which can be executed using a {@link JexlContext}
     * @throws JexlException if there is a problem parsing the script.
     */
    public abstract JexlScript createScript(String scriptText, String... names);

    /**
     * Creates a Script from a String containing valid JEXL syntax.
     * This method parses the script which validates the syntax.
     * It uses an array of parameter names that will be resolved during parsing;
     * a corresponding array of arguments containing values should be used during evaluation.
     *
     * @param scriptText A String containing valid JEXL syntax
     * @param info An info structure to carry debugging information if needed
     * @param names the script parameter names
     * @return A {@link JexlScript} which can be executed using a {@link JexlContext}
     * @throws JexlException if there is a problem parsing the script.
     */
    public abstract JexlScript createScript(String scriptText, JexlInfo info, String[] names);

    /**
     * Creates a Script from a {@link File} containing valid JEXL syntax.
     * This method parses the script and validates the syntax.
     *
     * @param scriptFile A {@link File} containing valid JEXL syntax. Must not be null. Must be a readable file.
     * @return A {@link JexlScript} which can be executed with a
     * {@link JexlContext}.
     * @throws IOException if there is a problem reading the script.
     * @throws JexlException if there is a problem parsing the script.
     */
    public abstract JexlScript createScript(File scriptFile) throws IOException;

    /**
     * Creates a Script from a {@link URL} containing valid JEXL syntax.
     * This method parses the script and validates the syntax.
     *
     * @param scriptUrl A {@link URL} containing valid JEXL syntax. Must not be null. Must be a readable file.
     * @return A {@link JexlScript} which can be executed with a
     * {@link JexlContext}.
     * @throws IOException if there is a problem reading the script.
     * @throws JexlException if there is a problem parsing the script.
     */
    public abstract JexlScript createScript(URL scriptUrl) throws IOException;

    /**
     * Accesses properties of a bean using an expression.
     * <p>
     * jexl.get(myobject, "foo.bar"); should equate to
     * myobject.getFoo().getBar(); (or myobject.getFoo().get("bar"))
     * </p>
     * <p>
     * If the JEXL engine is silent, errors will be logged through its logger as warning.
     * </p>
     *
     * @param bean the bean to get properties from
     * @param expr the property expression
     * @return the value of the property
     * @throws JexlException if there is an error parsing the expression or during evaluation
     */
    public abstract Object getProperty(Object bean, String expr);

    /**
     * Accesses properties of a bean using an expression.
     * <p>
     * If the JEXL engine is silent, errors will be logged through its logger as warning.
     * </p>
     *
     * @param context the evaluation context
     * @param bean the bean to get properties from
     * @param expr the property expression
     * @return the value of the property
     * @throws JexlException if there is an error parsing the expression or during evaluation
     */
    public abstract Object getProperty(JexlContext context, Object bean, String expr);

    /**
     * Assign properties of a bean using an expression.
     * <p>
     * jexl.set(myobject, "foo.bar", 10); should equate to
     * myobject.getFoo().setBar(10); (or myobject.getFoo().put("bar", 10) )
     * </p>
     * <p>
     * If the JEXL engine is silent, errors will be logged through its logger as warning.
     * </p>
     *
     * @param bean the bean to set properties in
     * @param expr the property expression
     * @param value the value of the property
     * @throws JexlException if there is an error parsing the expression or during evaluation
     */
    public abstract void setProperty(Object bean, String expr, Object value);

    /**
     * Assign properties of a bean using an expression. <p> If the JEXL engine is silent, errors will be logged through
     * its logger as warning. </p>
     *
     * @param context the evaluation context
     * @param bean the bean to set properties in
     * @param expr the property expression
     * @param value the value of the property
     * @throws JexlException if there is an error parsing the expression or during evaluation
     */
    public abstract void setProperty(JexlContext context, Object bean, String expr, Object value);

    /**
     * Invokes an object's method by name and arguments.
     * @param obj the method's invoker object
     * @param meth the method's name
     * @param args the method's arguments
     * @return the method returned value or null if it failed and engine is silent
     * @throws JexlException if method could not be found or failed and engine is not silent
     */
    public abstract Object invokeMethod(Object obj, String meth, Object... args);

    /**
     * Creates a new instance of an object using the most appropriate constructor based on the arguments.
     * @param <T> the type of object
     * @param clazz the class to instantiate
     * @param args the constructor arguments
     * @return the created object instance or null on failure when silent
     */
    public abstract <T> T newInstance(Class<? extends T> clazz, Object... args);

    /**
     * Creates a new instance of an object using the most appropriate constructor based on the arguments.
     * @param clazz the name of the class to instantiate resolved through this engine's class loader
     * @param args the constructor arguments
     * @return the created object instance or null on failure when silent
     */
    public abstract Object newInstance(String clazz, Object... args);

    /**
     * Trims the expression from front & ending spaces.
     * @param str expression to clean
     * @return trimmed expression ending in a semi-colon
     */
    public static String cleanExpression(CharSequence str) {
        if (str != null) {
            int start = 0;
            int end = str.length();
            if (end > 0) {
                // trim front spaces
                while (start < end && str.charAt(start) == ' ') {
                    ++start;
                }
                // trim ending spaces
                while (end > 0 && str.charAt(end - 1) == ' ') {
                    --end;
                }
                return str.subSequence(start, end).toString();
            }
            return "";
        }
        return null;
    }

    /**
     * Read from a reader into a local buffer and return a String with the contents of the reader.
     * @param scriptReader to be read.
     * @return the contents of the reader as a String.
     * @throws IOException on any error reading the reader.
     */
    public static String readerToString(Reader scriptReader) throws IOException {
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader;
        if (scriptReader instanceof BufferedReader) {
            reader = (BufferedReader) scriptReader;
        } else {
            reader = new BufferedReader(scriptReader);
        }
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append('\n');
            }
            return buffer.toString();
        } finally {
            try {
                reader.close();
            } catch (IOException xio) {
                // ignore
            }
        }

    }
}
