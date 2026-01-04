/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3.internal;

import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.JexlOptions;
import org.apache.commons.jexl3.JxltEngine;
import org.apache.commons.jexl3.internal.TemplateEngine.Block;
import org.apache.commons.jexl3.internal.TemplateEngine.BlockType;
import org.apache.commons.jexl3.internal.TemplateEngine.TemplateExpression;
import org.apache.commons.jexl3.parser.ASTArguments;
import org.apache.commons.jexl3.parser.ASTFunctionNode;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.ASTNumberLiteral;
import org.apache.commons.jexl3.parser.JexlNode;

/**
 * A Template instance.
 */
public final class TemplateScript implements JxltEngine.Template {

    /**
     * Collects the call-site surrounding a call to jexl:print(i).
     * <p>This allows parsing the blocks with the known symbols
     * in the frame visible to the parser.</p>
     *
     * @param node the visited node
     * @param callSites the map of printed expression number to node info
     */
    private static void collectPrintScope(final JexlNode node, final JexlNode.Info[] callSites) {
        final int nc = node.jjtGetNumChildren();
        if (node instanceof ASTFunctionNode && nc == 2) {
            // is child[0] jexl:print()?
            final ASTIdentifier nameNode = (ASTIdentifier) node.jjtGetChild(0);
            if ("print".equals(nameNode.getName()) && "jexl".equals(nameNode.getNamespace())) {
                // is there one argument?
                final ASTArguments argNode = (ASTArguments) node.jjtGetChild(1);
                if (argNode.jjtGetNumChildren() == 1) {
                    // seek the expression number
                    final JexlNode arg0 = argNode.jjtGetChild(0);
                    if (arg0 instanceof ASTNumberLiteral) {
                        final int exprNumber = ((ASTNumberLiteral) arg0).getLiteral().intValue();
                        callSites[exprNumber] = new JexlNode.Info(nameNode);
                        return;
                    }
                }
            }
        }
        for (int c = 0; c < nc; ++c) {
            collectPrintScope(node.jjtGetChild(c), callSites);
        }
    }

    /**
     * Gets the scope from a node info.
     *
     * @param info the node info
     * @param scope the outer scope
     * @return the scope
     */
    private static Scope scopeOf(final JexlNode.Info info, final Scope scope) {
        Scope found = null;
        JexlNode walk = info.getNode();
        while (walk != null) {
            if (walk instanceof ASTJexlScript) {
                found = ((ASTJexlScript) walk).getScope();
                break;
            }
            walk = walk.jjtGetParent();
        }
        return found != null ? found : scope;
    }

    /**
     * Creates the expression array from the list of blocks.
     *
     * @param scope the outer scope
     * @param blocks the list of blocks
     * @return the array of expressions
     */
    private TemplateExpression[] calleeScripts(final Scope scope, final Block[] blocks, final JexlNode.Info[] callSites) {
        final TemplateExpression[] expressions = new TemplateExpression[callSites.length];
        // jexl:print(...) expression counter
        int jpe = 0;
        // create the expressions using the intended scopes
        for (final Block block : blocks) {
            if (block.getType() == BlockType.VERBATIM) {
                final JexlNode.Info ji = callSites[jpe];
                // no node info means this verbatim is surrounded by comments markers;
                // expr at this index is never called
                final TemplateExpression te = ji != null
                    ? jxlt.parseExpression(ji, block.getBody(), scopeOf(ji, scope))
                    : jxlt.new ConstantExpression(block.getBody(), null);
                expressions[jpe++] = te;
            }
        }
        return expressions;
    }

    /**
     * Creates the script calling the list of blocks.
     * <p>This is used to create a script from a list of blocks
     * that were parsed from a template.</p>
     *
     * @param blocks the list of blocks
     * @return the script source
     */
    private static String callerScript(final Block[] blocks) {
        final StringBuilder strb = new StringBuilder();
        int nuexpr = 0;
        int line = 1;
        for (final Block block : blocks) {
            final int bl = block.getLine();
            while (line < bl) {
                strb.append("//\n");
                line += 1;
            }
            if (block.getType() == BlockType.VERBATIM) {
                strb.append("jexl:print(");
                strb.append(nuexpr++);
                strb.append(");\n");
                line += 1;
            } else {
                final String body = block.getBody();
                strb.append(body);
                // keep track of the line number
                for (int c = 0; c < body.length(); ++c) {
                    if (body.charAt(c) == '\n') {
                        line += 1;
                    }
                }
            }
        }
        return strb.toString();
    }

    /** The prefix marker. */
    private final String prefix;

    /** The array of source blocks. */
    private final Block[] source;

    /** The resulting script. */
    private final ASTJexlScript script;

    /** The TemplateEngine expressions called by the script. */
    private final TemplateExpression[] exprs;

    /** The engine. */
    private final TemplateEngine jxlt;

    /**
     * Creates a new template from an character input.
     *
     * @param engine the template engine
     * @param jexlInfo the source info
     * @param directive the prefix for lines of code; cannot be "$", "${", "#" or "#{"
                  since this would preclude being able to differentiate directives and jxlt expressions
     * @param reader    the input reader
     * @param parms     the parameter names
     * @throws NullPointerException     if either the directive prefix or input is null
     * @throws IllegalArgumentException if the directive prefix is invalid
     */
    public TemplateScript(final TemplateEngine engine,
                          final JexlInfo jexlInfo,
                          final String directive,
                          final Reader reader,
                          final String... parms) {
        Objects.requireNonNull(directive, "directive");
        final String engineImmediateCharString = Character.toString(engine.getImmediateChar());
        final String engineDeferredCharString = Character.toString(engine.getDeferredChar());

        if (engineImmediateCharString.equals(directive)
                || engineDeferredCharString.equals(directive)
                || (engineImmediateCharString + "{").equals(directive)
                || (engineDeferredCharString + "{").equals(directive)) {
            throw new IllegalArgumentException(directive + ": is not a valid directive pattern");
        }
        Objects.requireNonNull(reader, "reader");
        this.jxlt = engine;
        this.prefix = directive;
        final Engine jexl = jxlt.getEngine();
        // create the caller script
        final Block[] blocks = jxlt.readTemplate(prefix, reader).toArray(new Block[0]);
        int verbatims = 0;
        for(final Block b : blocks) {
            if (BlockType.VERBATIM == b.getType()) {
                verbatims += 1;
            }
        }
        final String scriptSource = callerScript(blocks);
        // allow lambda defining params
        final JexlInfo info = jexlInfo == null ? jexl.createInfo() : jexlInfo;
        final Scope scope = parms == null ? null : new Scope(null, parms);
        final ASTJexlScript callerScript = jexl.jxltParse(info.at(1, 1), false, scriptSource, scope).script();
        // seek the map of expression number to scope so we can parse Unified
        // expression blocks with the appropriate symbols
        final JexlNode.Info[] callSites = new JexlNode.Info[verbatims];
        collectPrintScope(callerScript.script(), callSites);
        // create the expressions from the blocks
        this.exprs = calleeScripts(scope, blocks, callSites);
        this.script = callerScript;
        this.source = blocks;
    }

    /**
     * Private ctor used to expand deferred expressions during prepare.
     *
     * @param engine    the template engine
     * @param thePrefix the directive prefix
     * @param theSource the source
     * @param theScript the script
     * @param theExprs  the expressions
     */
    TemplateScript(final TemplateEngine engine,
                   final String thePrefix,
                   final Block[] theSource,
                   final ASTJexlScript theScript,
                   final TemplateExpression[] theExprs) {
        jxlt = engine;
        prefix = thePrefix;
        source = theSource;
        script = theScript;
        exprs = theExprs;
    }

    @Override
    public String asString() {
        final StringBuilder strb = new StringBuilder();
        int e = 0;
        for (final Block block : source) {
            if (block.getType() == BlockType.DIRECTIVE) {
                strb.append(prefix);
                strb.append(block.getBody());
            } else {
                exprs[e++].asString(strb);
            }
        }
        return strb.toString();
    }

    @Override
    public void evaluate(final JexlContext context, final Writer writer) {
        evaluate(context, writer, (Object[]) null);
    }

    @Override
    public void evaluate(final JexlContext context, final Writer writer, final Object... args) {
        final Engine jexl = jxlt.getEngine();
        final JexlOptions options = jexl.evalOptions(script, context);
        final Frame frame = script.createFrame(args);
        final TemplateInterpreter.Arguments targs = new TemplateInterpreter
                .Arguments(jexl)
                .context(context)
                .options(options)
                .frame(frame)
                .expressions(exprs)
                .writer(writer);
        final Interpreter interpreter = jexl.createTemplateInterpreter(targs);
        interpreter.interpret(script);
    }

    /**
     * @return exprs
     */
    TemplateExpression[] getExpressions() {
        return exprs;
    }

    @Override
    public String[] getParameters() {
        return script.getParameters();
    }

    @Override
    public Map<String, Object> getPragmas() {
        return script.getPragmas();
    }

    /**
     * @return script
     */
    ASTJexlScript getScript() {
        return script;
    }

    @Override
    public Set<List<String>> getVariables() {
        final Engine.VarCollector collector = jxlt.getEngine().varCollector();
        for (final TemplateExpression expr : exprs) {
            expr.getVariables(collector);
        }
        return collector.collected();
    }

    @Override
    public TemplateScript prepare(final JexlContext context) {
        final Engine jexl = jxlt.getEngine();
        final JexlOptions options = jexl.evalOptions(script, context);
        final Frame frame = script.createFrame((Object[]) null);
        final TemplateInterpreter.Arguments targs = new TemplateInterpreter
                .Arguments(jxlt.getEngine())
                .context(context)
                .options(options)
                .frame(frame);
        final Interpreter interpreter = jexl.createTemplateInterpreter(targs);
        final TemplateExpression[] immediates = new TemplateExpression[exprs.length];
        for (int e = 0; e < exprs.length; ++e) {
            try {
                immediates[e] = exprs[e].prepare(interpreter);
            } catch (final JexlException xjexl) {
                final JexlException xuel = TemplateEngine.createException(xjexl.getInfo(), "prepare", exprs[e], xjexl);
                if (jexl.isSilent()) {
                    if (jexl.logger.isWarnEnabled()) {
                        jexl.logger.warn(xuel.getMessage(), xuel.getCause());
                    }
                    return null;
                }
                throw xuel;
            }
        }
        return new TemplateScript(jxlt, prefix, source, script, immediates);
    }

    @Override
    public String toString() {
        final StringBuilder strb = new StringBuilder();
        for (final Block block : source) {
            block.toString(strb, prefix);
        }
        return strb.toString();
    }
}
