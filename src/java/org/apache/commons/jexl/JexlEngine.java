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
package org.apache.commons.jexl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Reader;
import java.util.Map;
import java.util.Collections;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.commons.jexl.parser.ParseException;
import org.apache.commons.jexl.parser.Parser;
import org.apache.commons.jexl.parser.SimpleNode;
import org.apache.commons.jexl.parser.TokenMgrError;
import org.apache.commons.jexl.parser.ASTJexlScript;
import org.apache.commons.jexl.util.Introspector;
import org.apache.commons.jexl.util.introspection.Uberspect;

/**
 * <p>
 * Creates and evaluates Expression and Script objects.
 * Determines the behavior of Expressions & Scripts during their evaluation with respect to:
 * <ul>
 *  <li>Introspection, see {@link Uberspect}</li>
 *  <li>Arithmetic & comparison, see {@link Arithmetic}</li>
 *  <li>Error reporting</li>
 *  <li>Logging</li>
 * </ul>
 * </p>
 * <p>The <code>setSilent</code>and<code>setLenient</code> methods allow to fine-tune an engine instance behavior according to
 * various error control needs.
 * </p>
 * <ul>
 * <li>When "silent" & "lenient" (not-strict):
 * <p> 0 & null should be indicators of "default" values so that even in an case of error,
 * something meaningfull can still be inferred; may be convenient for configurations.
 * </p>
 * </li>
 * <li>When "silent" & "strict":
 * <p>One should probably consider using null as an error case - ie, every object
 * manipulated by JEXL should be valued; the ternary operator, especially the '?:' form
 * can be used to workaround exceptional cases.
 * Use case could be configuration with no implicit values or defaults.
 * </p>
 * </li>
 * <li>When "not-silent" & "not-strict":
 * <p>The error control grain is roughly on par with JEXL 1.0</p>
 * </li>
 * <li>When "not-silent" & "strict":
 * <p>The finest error control grain is obtained; it is the closest to Java code -
 * still augmented by "script" capabilities regarding automated conversions & type matching.
 * </p>
 * </li>
 * </ul>
 * <p>
 * Note that methods that evaluate expressions may throw <em>unchecked</em> exceptions;
 * The {@link JexlException} are thrown in "non-silent" mode but since these are
 * RuntimeException, user-code <em>should</em> catch them wherever most appropriate.
 * </p>
 * @since 2.0
 */
public class JexlEngine {
    /**
     * The Uberspect instance.
     */
    protected final Uberspect uberspect;
    /**
     * The Arithmetic instance.
     */
    protected final Arithmetic arithmetic;
    /**
     * The Log to which all JexlEngine messages will be logged.
     */
    protected final Log LOG;
    /**
     * The singleton ExpressionFactory also holds a single instance of
     * {@link Parser}.
     * When parsing expressions, ExpressionFactory synchronizes on Parser.
     */
    protected final Parser parser = new Parser(new StringReader(";")); //$NON-NLS-1$

    /**
     * Whether expressions evaluated by this engine will throw exceptions or 
     * return null.
     */
    protected boolean silent = false;

    /**
     *  The map of 'prefix:function' to object implementing the function.
     */
    protected Map<String,Object> functions = Collections.EMPTY_MAP;

    /**
     * The expression cache.
     */
    protected Map<String,SimpleNode> cache = null;

    /**
     * An empty/static/non-mutable JexlContext used instead of null context
     */
    protected static final JexlContext EMPTY_CONTEXT = new JexlContext() {
        public void setVars(Map vars) {
            throw new UnsupportedOperationException("Immutable JexlContext");
        }
        public Map getVars() {
            return java.util.Collections.EMPTY_MAP;
        }
    };

    /**
     * Creates a default engine
     */
    public JexlEngine() {
        this(null, null, null, null);
    }

    /**
     * Creates a JEXL engine using the provided {@link Uberspect}, (@link Arithmetic) and logger.
     * @param uberspect to allow different introspection behaviour
     * @param arithmetic to allow different arithmetic behaviour
     * @param funcs an optional map of functions (@see setFunctions)
     * @param log the logger for various messages
     */
    public JexlEngine(Uberspect uberspect, Arithmetic arithmetic, Map<String,Object> funcs, Log log) {
        this.uberspect = uberspect == null? Introspector.getUberspect() : uberspect;
        this.arithmetic = arithmetic == null? new JexlArithmetic(true) : arithmetic;
        if (funcs != null) {
            this.functions = funcs;
        }
        if (log == null) {
            log = LogFactory.getLog(JexlEngine.class);
        }
        if (log == null) {
            throw new NullPointerException("logger can not be null");
        }
        this.LOG = log;
    }
    
    /**
     * Sets whether this engine throws JexlException during evaluation when an error is triggered.
     * @param silent true means no JexlException will occur, false allows them
     */
    public void setSilent(boolean silent) {
        this.silent = silent;
    }
    
    /**
     * Checks whether this engine throws JexlException during evaluation.
     */
    public boolean isSilent() {
        return this.silent;
    }

    /**
     * Sets whether this engine triggers errors during evaluation when null is used as
     * an operand.
     * @param lenient true means no JexlException will occur, false allows them
     */
    public void setLenient(boolean lenient) {
        this.arithmetic.setLenient(lenient);
    }

    /**
     * Checks whether this engine triggers errors during evaluation when null is used as
     * an operand.
     */
    public boolean isLenient() {
        return this.arithmetic.isLenient();
    }
    /**
     * Sets a cache of the defined size for expressions.
     * @param size if not strictly positive, no cache is used.
     */
    public void setCache(int size) {
        // since the cache is only used during parse, use same sync object
        synchronized(parser) {
            if (size <= 0) {
                cache = null;
            }
            else if (cache == null || cache.size() != size) {
                cache = createCache(size);
            }
        }
    }

    /**
     * Sets the map of function namespaces.
     * <p>
     * It should be defined once and not modified afterwards since it might be shared
     * between multiple engines evaluating expressions concurrently.
     * </p>
     * <p>
     * Each entry key is used as a prefix, each entry value used as a bean implementing
     * methods; an expression like 'nsx:method(123)' will thus be solved by looking at
     * a registered bean named 'nsx' that implements method 'method' in that map.
     * If all methods are static, you may use the bean class instead of an instance as value.
     * </p>
     * <p>
     * The key or prefix allows to retrieve the bean that plays the role of the namespace.
     * If the prefix is null, the namespace is the top-level namespace allowing to define
     * top-level user defined functions ( ie: myfunc(...) )
     * </p>
     * <p>
     * Note that you can always use a variable implementing methods & use
     * the 'var.func(...)' syntax if you need more dynamic constructs.
     * </p>
     * @param funcs the map of functions that should not mutate after the call; if null
     * is passed, the empty collection is used.
     */
    public void setFunctions(Map<String, Object> funcs) {
        functions = funcs != null? funcs : Collections.EMPTY_MAP;
    }


    /**
     * Retrieves the map of function namespaces.
     *
     * @return the map passed in setFunctions or the empty map if the
     * original was null.
     */
    public Map<String, Object> getFunctions() {
        return functions;
    }

    /**
     * Creates an Expression from a String containing valid
     * JEXL syntax.  This method parses the expression which
     * must contain either a reference or an expression.
     * @param expression A String containing valid JEXL syntax
     * @return An Expression object which can be evaluated with a JexlContext
     * @throws ParseException An exception can be thrown if there is a problem
     *      parsing this expression, or if the expression is neither an
     *      expression or a reference.
     */
    public Expression createExpression(String expression)
        throws ParseException {
        // Parse the expression
        SimpleNode tree = parse(expression);
        if (tree.jjtGetNumChildren() > 1) {
            LOG.warn("The JEXL Expression created will be a reference"
                + " to the first expression from the supplied script: \""
                + expression + "\" ");
        }
        SimpleNode node = (SimpleNode) tree.jjtGetChild(0);
        return new ExpressionImpl(this, expression, node);
    }

    /**
     * Creates a Script from a String containing valid JEXL syntax.
     * This method parses the script which validates the syntax.
     *
     * @param scriptText A String containing valid JEXL syntax
     * @return A {@link Script} which can be executed using a {@link JexlContext}.
     * @throws ParseException if there is a problem parsing the script.
     */
    public Script createScript(String scriptText) throws ParseException {
        if (scriptText == null) {
            throw new NullPointerException("scriptText is null");
        }
        SimpleNode script = parse(scriptText);
        if (script instanceof ASTJexlScript) {
            return new ScriptImpl(this, scriptText, (ASTJexlScript) script);
        } else {
            throw new IllegalStateException("Parsed script is not an ASTJexlScript");
        }
    }

    /**
     * Creates a Script from a {@link File} containing valid JEXL syntax.
     * This method parses the script and validates the syntax.
     *
     * @param scriptFile A {@link File} containing valid JEXL syntax.
     *      Must not be null. Must be a readable file.
     * @return A {@link Script} which can be executed with a
     *      {@link JexlContext}.
     * @throws IOException if there is a problem reading the script.
     * @throws ParseException if there is a problem parsing the script.
     */
    public Script createScript(File scriptFile) throws ParseException, IOException {
        if (scriptFile == null) {
            throw new NullPointerException("scriptFile is null");
        }
        if (!scriptFile.canRead()) {
            throw new IOException("Can't read scriptFile ("
                + scriptFile.getCanonicalPath() + ")");
        }
        BufferedReader reader = new BufferedReader(new FileReader(scriptFile));
        return createScript(readerToString(reader));

    }

    /**
     * Creates a Script from a {@link URL} containing valid JEXL syntax.
     * This method parses the script and validates the syntax.
     *
     * @param scriptUrl A {@link URL} containing valid JEXL syntax.
     *      Must not be null. Must be a readable file.
     * @return A {@link Script} which can be executed with a
     *      {@link JexlContext}.
     * @throws IOException if there is a problem reading the script.
     * @throws ParseException if there is a problem parsing the script.
     */
    public Script createScript(URL scriptUrl) throws ParseException, IOException {
        if (scriptUrl == null) {
            throw new NullPointerException("scriptUrl is null");
        }
        URLConnection connection = scriptUrl.openConnection();

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream()));
        return createScript(readerToString(reader));
    }

    /**
     * Access properties of a bean using an expression.
     * <p>
     * jexl.get(myobject, "foo.bar"); should equate to
     * myobject.getFoo().getBar(); (or myobject.getFoo().get("bar"))
     * </p>
     * <p>
     * If the JEXL engine is silent, errors will be logged through its logger as info.
     * </p>
     * @param bean the bean to get properties from
     * @param expr the property expression
     * @return the value of the property
     * @throws JexlException if there is an error parsing the expression or during evaluation
     */
    public Object getProperty(Object bean, String expr) {
        return getProperty(EMPTY_CONTEXT, bean, expr);
    }
    public Object getProperty(JexlContext context, Object bean, String expr) {
        Map<String, Object> vars = context.getVars();
        // lets build 1 unique & unused identifiers wrt context
        String r0 = "$0";
        for(int s = 0; vars.containsKey(r0); ++s) r0 = r0 + s;
        expr = r0 + (expr.charAt(0) =='['? "" : "." )+ expr +";";
        try {
            SimpleNode tree = parse(expr);
            SimpleNode node = (SimpleNode) tree.jjtGetChild(0).jjtGetChild(0);
            Interpreter interpreter = createInterpreter(context);
            // ensure 4 objects in register array
            Object[] r = {r0, bean, r0, bean};
            interpreter.setRegisters(r);
            return node.jjtAccept(interpreter, null);
        }
        catch(JexlException xjexl) {
            if (silent) {
                LOG.warn(xjexl.getMessage(), xjexl.getCause());
                return null;
            }
            throw xjexl;
        }
        catch(ParseException xparse) {
            if (silent) {
                LOG.warn(xparse.getMessage(), xparse.getCause());
                return null;
            }
            throw new JexlException(null, "parsing error", xparse);
        }
    }

    /**
     * Assign properties of a bean using an expression.
     * <p>
     * jexl.set(myobject, "foo.bar", 10); should equate to
     * myobject.getFoo().setBar(10); (or myobject.getFoo().put("bar", 10) )
     * </p>
     * <p>
     * If the JEXL engine is silent, errors will be logged through its logger as info.
     * </p>
     * @param bean the bean to set properties in
     * @param expr the property expression
     * @param value the value of the property
     * @throws JexlException if there is an error parsing the expression or during evaluation
     */
    public void setProperty(Object bean, String expr, Object value) {
       setProperty(EMPTY_CONTEXT, bean, expr, value);
    }
    public void setProperty(JexlContext context, Object bean, String expr, Object value) {
        Map<String, Object> vars = context.getVars();
        // lets build 2 unique & unused identifiers wrt context
        String r0 = "$0", r1 = "$1";
        for(int s = 0; vars.containsKey(r0); ++s) r0 = r0 + s;
        for(int s = 0; vars.containsKey(r1); ++s) r1 = r1 + s;
        // synthetize expr
        expr = r0 + (expr.charAt(0) =='['? "" : "." )+ expr +"=" + r1 +";";
        try {
            SimpleNode tree = parse(expr);
            SimpleNode node = (SimpleNode) tree.jjtGetChild(0).jjtGetChild(0);
            Interpreter interpreter = createInterpreter(context);
            // set the registers
            Object[] r = {r0, bean, r1, value};
            interpreter.setRegisters(r);
            node.jjtAccept(interpreter, null);
        }
        catch(JexlException xjexl) {
            if (silent) {
                LOG.warn(xjexl.getMessage(), xjexl.getCause());
                return;
            }
            throw xjexl;
        }
        catch(ParseException xparse) {
            if (silent) {
                LOG.warn(xparse.getMessage(), xparse.getCause());
                return;
            }
            throw new JexlException(null, "parsing error", xparse);
        }
    }
    
    /**
     * Creates an interpreter.
     * @param context a JexlContext; if null, the EMPTY_CONTEXT is used instead.
     * @return an Interpreter
     */
    protected Interpreter createInterpreter(JexlContext context) {
        if (context == null) context = EMPTY_CONTEXT;
        return new Interpreter(this, context);
    }
    
    /**
     * Creates a SimpleNode cache.
     * @param cacheSize the cache size, must be > 0
     * @return a Map usable as a cache bounded to the given size
     */
    protected Map<String,SimpleNode> createCache(final int cacheSize) {
        return new java.util.LinkedHashMap<String,SimpleNode>(cacheSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > cacheSize;
            }
        };
    }

    /**
     * Parses an expression.
     * @param expression
     * @return the parsed tree
     * @throws ParseException if any error occured during parsing
     */
    protected SimpleNode parse(CharSequence expression) throws ParseException {
        String expr = cleanExpression(expression);
        SimpleNode tree = null;
        synchronized (parser) {
            LOG.debug("Parsing expression: " + expression);
            if (cache != null) {
                tree = cache.get(expr);
                if (tree != null) {
                    return tree;
                }
            }
            try {
                Reader reader = expr.endsWith(";")?
                            new StringReader(expr) :
                            new StringReader(expr + ";");
                tree = parser.parse(reader);
                if (cache != null) {
                    cache.put(expr, tree);
                }
            } catch (TokenMgrError tme) {
                throw new ParseException(tme.getMessage());
            } catch (ParseException e) {
                throw e;
            }
        }
        return tree;
    }

    /**
     * Trims the expression from front & ending spaces.
     * @param str expression to clean
     * @return trimmed expression ending in a semi-colon
     */
    protected String cleanExpression(CharSequence str) {
        if (str != null) {
            int start = 0;
            int end = str.length();
            if (end > 0) {
                for(start = 0; start < end && str.charAt(start) == ' '; ++start) {} // trim front spaces
                for(;end > 0 && str.charAt(end - 1) == ' '; --end) {} // trim ending spaces
                return str.subSequence(start, end).toString();
            }
            return "";
        }
        return null;
    }
    
    /**
     * Read a buffered reader into a StringBuffer and return a String with
     * the contents of the reader.
     * @param reader to be read.
     * @return the contents of the reader as a String.
     * @throws IOException on any error reading the reader.
     */
    protected static String readerToString(BufferedReader reader)
        throws IOException {
        StringBuilder buffer = new StringBuilder();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append('\n');
            }
            return buffer.toString();
        } finally {
            reader.close();
        }

    }

    /**
     * ExpressionFactory & ScriptFactory need a singleton and this is the package
     * instance fulfilling that pattern.
     */
    @Deprecated
    private static volatile JexlEngine DEFAULT = null;
    /**
     * Retrieves a default JEXL engine.
     * @return the singleton
     */
    @Deprecated
    static JexlEngine getDefault() {
        // java 5 allows the lazy singleton initialization
        // using a double-check locking pattern
        if (DEFAULT == null) {
            synchronized(JexlEngine.class) {
                if (DEFAULT == null) {
                    DEFAULT = new JexlEngine();
                }
            }
        }
        return DEFAULT;
    }
}