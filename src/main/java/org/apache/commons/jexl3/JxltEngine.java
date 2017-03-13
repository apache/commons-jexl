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

import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A simple "JeXL Template" engine.
 *
 * <p>At the base is an evaluator similar to the Unified EL evaluator used in JSP/JSF based on JEXL.
 * At the top is a template engine inspired by Velocity that uses JEXL (instead of OGNL/VTL) as the scripting
 * language.</p>
 *
 * <p>The evaluator is intended to be used in configuration modules, XML based frameworks or JSP taglibs
 * and facilitate the implementation of expression evaluation.</p>
 *
 * <p>The template engine is intended to output any form of text; html, XML, CSV...</p>
 *
 * @since 3.0
 */
public abstract class JxltEngine {

    /**
     * The sole type of (runtime) exception the JxltEngine can throw.
     */
    public static class Exception extends JexlException {

        /** Serial version UID. */
        private static final long serialVersionUID = 201112030113L;

        /**
         * Creates an Exception.
         *
         * @param info the contextual information
         * @param msg the exception message
         * @param cause the exception cause
         */
        public Exception(JexlInfo info, String msg, Throwable cause) {
            super(info, msg, cause);
        }
    }

    /**
     * A unified expression that can mix immediate, deferred and nested sub-expressions as well as string constants;
     * <ul>
     *   <li>The "immediate" syntax is of the form <code>"...${jexl-expr}..."</code></li>
     *   <li>The "deferred" syntax is of the form <code>"...#{jexl-expr}..."</code></li>
     *   <li>The "nested" syntax is of the form <code>"...#{...${jexl-expr0}...}..."</code></li>
     *   <li>The "composite" syntax is of the form <code>"...${jexl-expr0}... #{jexl-expr1}..."</code></li>
     * </ul>
     *
     * <p>Deferred and immediate expression carry different intentions:</p>
     *
     * <ul>
     *   <li>An immediate expression indicate that evaluation is intended to be performed close to
     *       the definition/parsing point.</li>
     *   <li>A deferred expression indicate that evaluation is intended to occur at a later stage.</li>
     * </ul>
     *
     * <p>For instance: <code>"Hello ${name}, now is #{time}"</code> is a composite "deferred" expression since one
     * of its subexpressions is deferred. Furthermore, this (composite) expression intent is
     * to perform two evaluations; one close to its definition and another one in a later
     * phase.</p>
     *
     * <p>The API reflects this feature in 2 methods, prepare and evaluate. The prepare method
     * will evaluate the immediate subexpression and return an expression that contains only
     * the deferred subexpressions (and constants), a prepared expression. Such a prepared expression
     * is suitable for a later phase evaluation that may occur with a different JexlContext.
     * Note that it is valid to call evaluate without prepare in which case the same JexlContext
     * is used for the 2 evaluation phases.</p>
     *
     * <p>In the most common use-case where deferred expressions are to be kept around as properties of objects,
     * one should createExpression and prepare an expression before storing it and evaluate it each time
     * the property storing it is accessed.</p>
     *
     * <p>Note that nested expression use the JEXL syntax as in:</p>
     *
     * <blockquote><code>"#{${bar}+'.charAt(2)'}"</code></blockquote>
     *
     * <p>The most common mistake leading to an invalid expression being the following:</p>
     *
     * <blockquote><code>"#{${bar}charAt(2)}"</code></blockquote>
     *
     * <p>Also note that methods that createExpression evaluate expressions may throw <em>unchecked</em> exceptions;
     * The {@link JxltEngine.Exception} are thrown when the engine instance is in "non-silent" mode
     * but since these are RuntimeException, user-code <em>should</em> catch them where appropriate.</p>
     *
     * @since 2.0
     */
    public interface Expression {

        /**
         * Generates this expression's string representation.
         *
         * @return the string representation
         */
        String asString();

        /**
         * Adds this expression's string representation to a StringBuilder.
         *
         * @param strb the builder to fill
         * @return the builder argument
         */
        StringBuilder asString(StringBuilder strb);

        /**
         * Evaluates this expression.
         *
         * <p>If the underlying JEXL engine is silent, errors will be logged through its logger as warning.</p>
         *
         * @param context the variable context
         * @return the result of this expression evaluation or null if an error occurs and the {@link JexlEngine} is
         * running in silent mode
         * @throws Exception if an error occurs and the {@link JexlEngine}
         * is not silent
         */
        Object evaluate(JexlContext context);

        /**
         * Retrieves this expression's source expression.
         * <p>
         * If this expression was prepared, this allows to retrieve the
         * original expression that lead to it.</p>
         * <p>Other expressions return themselves.</p>
         *
         * @return the source expression
         */
        Expression getSource();

        /**
         * Gets the list of variables accessed by this expression.
         * <p>This method will visit all nodes of the sub-expressions and extract all variables whether they
         * are written in 'dot' or 'bracketed' notation. (a.b is equivalent to a['b']).</p>
         *
         * @return the set of variables, each as a list of strings (ant-ish variables use more than 1 string)
         * or the empty set if no variables are used
         */
        Set<List<String>> getVariables();

        /**
         * Checks whether this expression is deferred.
         *
         * @return true if deferred, false otherwise
         */
        boolean isDeferred();

        /**
         * Checks whether this expression is immediate.
         *
         * @return true if immediate, false otherwise
         */
        boolean isImmediate();

        /**
         * Evaluates the immediate sub-expressions.
         *
         * <p>When the expression is dependant upon immediate and deferred sub-expressions,
         * evaluates the immediate sub-expressions with the context passed as parameter
         * and returns this expression deferred form.</p>
         *
         * <p>In effect, this binds the result of the immediate sub-expressions evaluation in the
         * context, allowing to differ evaluation of the remaining (deferred) expression within another context.
         * This only has an effect to nested and composite expressions that contain differed and
         * immediate sub-expressions.</p>
         *
         * <p>If the underlying JEXL engine is silent, errors will be logged through its logger as warning.* </p>
         *
         * @param context the context to use for immediate expression evaluations
         * @return an {@link Expression} or null if an error occurs and the {@link JexlEngine} is running
         * in silent mode
         * @throws Exception if an error occurs and the {@link JexlEngine} is not in silent mode
         */
        Expression prepare(JexlContext context);

        /**
         * Formats this expression, adding its source string representation in
         * comments if available: 'expression /*= source *\/'' .
         *
         * @return the formatted expression string
         */
        @Override
        String toString();
    }

    /**
     * Creates a a {@link Expression} from an expression string.
     * Uses and fills up the expression cache if any.
     *
     * <p>If the underlying JEXL engine is silent, errors will be logged through its logger as warnings.</p>
     *
     * @param expression the {@link Template} string expression
     * @return the {@link Expression}, null if silent and an error occurred
     * @throws Exception if an error occurs and the {@link JexlEngine} is not silent
     */
    public Expression createExpression(String expression) {
        return createExpression(null, expression);
    }

    /**
     * Creates a a {@link Expression} from an expression string.
     * Uses and fills up the expression cache if any.
     *
     * <p>If the underlying JEXL engine is silent, errors will be logged through its logger as warnings.</p>
     *
     * @param info the {@link JexlInfo} source information
     * @param expression the {@link Template} string expression
     * @return the {@link Expression}, null if silent and an error occured
     * @throws Exception if an error occurs and the {@link JexlEngine} is not silent
     */
    public abstract Expression createExpression(JexlInfo info, String expression);

    /**
     * A template is a JEXL script that evaluates by writing its content through a Writer.
     * <p>
     * The source text is parsed considering each line beginning with '$$' (as default pattern) as JEXL script code
     * and all others as Unified JEXL expressions; those expressions will be invoked from the script during
     * evaluation and their output gathered through a writer.
     * It is thus possible to use looping or conditional construct "around" expressions generating output.
     * </p>
     * For instance:
     * <blockquote><pre>
     * $$ for(var x : [1, 3, 5, 42, 169]) {
     * $$   if (x == 42) {
     * Life, the universe, and everything
     * $$   } else if (x &gt; 42) {
     * The value $(x} is over fourty-two
     * $$   } else {
     * The value ${x} is under fourty-two
     * $$   }
     * $$ }
     * </pre></blockquote>
     *
     * <p>Will evaluate as:</p>
     *
     * <blockquote><pre>
     * The value 1 is under fourty-two
     * The value 3 is under fourty-two
     * The value 5 is under fourty-two
     * Life, the universe, and everything
     * The value 169 is over fourty-two
     * </pre></blockquote>
     *
     * <p>During evaluation, the template context exposes its writer as '$jexl' which is safe to use in this case.
     * This allows writing directly through the writer without adding new-lines as in:</p>
     *
     * <blockquote><pre>
     * $$ for(var cell : cells) { $jexl.print(cell); $jexl.print(';') }
     * </pre></blockquote>
     *
     * <p>A template is expanded as one JEXL script and a list of template expressions; each template expression is
     * being replaced in the script by a call to jexl:print(expr) (the expr is in fact the expr number in the template).
     * This integration uses a specialized JexlContext (TemplateContext) that serves as a namespace (for jexl:)
     * and stores the template expression array and the writer (java.io.Writer) that the 'jexl:print(...)'
     * delegates the output generation to.</p>
     *
     * @since 3.0
     */
    public interface Template {

        /**
         * Recreate the template source from its inner components.
         *
         * @return the template source rewritten
         */
        String asString();

        /**
         * Evaluates this template.
         *
         * @param context the context to use during evaluation
         * @param writer the writer to use for output
         */
        void evaluate(JexlContext context, Writer writer);

        /**
         * Evaluates this template.
         *
         * @param context the context to use during evaluation
         * @param writer the writer to use for output
         * @param args the arguments
         */
        void evaluate(JexlContext context, Writer writer, Object... args);

        /**
         * Prepares this template by expanding any contained deferred TemplateExpression.
         *
         * @param context the context to prepare against
         * @return the prepared version of the template
         */
        Template prepare(JexlContext context);

        /**
         * Gets the list of variables accessed by this template.
         * <p>This method will visit all nodes of the sub-expressions and extract all variables whether they
         * are written in 'dot' or 'bracketed' notation. (a.b is equivalent to a['b']).</p>
         *
         * @return the set of variables, each as a list of strings (ant-ish variables use more than 1 string)
         * or the empty set if no variables are used
         */
        Set<List<String>> getVariables();

        /**
         * Gets the list of parameters expected by this template.
         *
         * @return the parameter names array
         */
        String[] getParameters();

        /**
         * Gets this script pragmas.
         *
         * @return the (non null, may be empty) pragmas map
         * @since 3.1
         */
        Map<String, Object> getPragmas();
    }

    /**
     * Creates a new template.
     *
     * @param info the jexl info (file, line, column)
     * @param prefix the directive prefix
     * @param source the source
     * @param parms the parameter names
     * @return the template
     */
    public abstract Template createTemplate(JexlInfo info, String prefix, Reader source, String... parms);

    /**
     * Creates a new template.
     *
     * @param info the source info
     * @param parms the parameter names
     * @param source the source
     * @return the template
     */
    public Template createTemplate(JexlInfo info, String source, String... parms) {
        return createTemplate(info, "$$", new StringReader(source), parms);
    }

    /**
     * Creates a new template.
     *
     * @param info the source info
     * @param source the source
     * @return the template
     */
    public Template createTemplate(JexlInfo info, String source) {
        return createTemplate(info, "$$", new StringReader(source), (String[]) null);
    }

    /**
     * Creates a new template.
     *
     * @param prefix the directive prefix
     * @param source the source
     * @param parms the parameter names
     * @return the template
     */
    public Template createTemplate(String prefix, Reader source, String... parms) {
        return createTemplate(null, prefix, source, parms);
    }

    /**
     * Creates a new template.
     *
     * @param source the source
     * @param parms the parameter names
     * @return the template
     */
    public Template createTemplate(String source, String... parms) {
        return createTemplate(null, source, parms);
    }

    /**
     * Creates a new template.
     *
     * @param source the source
     * @return the template
     */
    public Template createTemplate(String source) {
        return createTemplate(null, source);
    }

    /**
     * Gets the {@link JexlEngine} underlying this template engine.
     *
     * @return the JexlEngine
     */
    public abstract JexlEngine getEngine();

    /**
     * Clears the cache.
     */
    public abstract void clearCache();
}
