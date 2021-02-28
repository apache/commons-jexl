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
package org.apache.commons.jexl3.internal;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.JxltEngine;
import org.apache.commons.jexl3.internal.TemplateEngine.Block;
import org.apache.commons.jexl3.internal.TemplateEngine.BlockType;
import org.apache.commons.jexl3.internal.TemplateEngine.TemplateExpression;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlOptions;
import org.apache.commons.jexl3.parser.ASTArguments;
import org.apache.commons.jexl3.parser.ASTFunctionNode;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTNumberLiteral;
import org.apache.commons.jexl3.parser.JexlNode;

import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A Template instance.
 */
public final class TemplateScript implements JxltEngine.Template {
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
     * @param engine the template engine
     * @param info the source info
     * @param directive the prefix for lines of code; can not be "$", "${", "#" or "#{"
                  since this would preclude being able to differentiate directives and jxlt expressions
     * @param reader    the input reader
     * @param parms     the parameter names
     * @throws NullPointerException     if either the directive prefix or input is null
     * @throws IllegalArgumentException if the directive prefix is invalid
     */
    public TemplateScript(final TemplateEngine engine,
                          JexlInfo info,
                          final String directive,
                          final Reader reader,
                          final String... parms) {
        if (directive == null) {
            throw new NullPointerException("null prefix");
        }
        final String engineImmediateCharString = Character.toString(engine.getImmediateChar());
        final String engineDeferredCharString = Character.toString(engine.getDeferredChar());

        if (engineImmediateCharString.equals(directive)
                || engineDeferredCharString.equals(directive)
                || (engineImmediateCharString + "{").equals(directive)
                || (engineDeferredCharString + "{").equals(directive)) {
            throw new IllegalArgumentException(directive + ": is not a valid directive pattern");
        }
        if (reader == null) {
            throw new NullPointerException("null input");
        }
        this.jxlt = engine;
        this.prefix = directive;
        final List<Block> blocks = jxlt.readTemplate(prefix, reader);
        final List<TemplateExpression> uexprs = new ArrayList<>();
        final StringBuilder strb = new StringBuilder();
        int nuexpr = 0;
        int codeStart = -1;
        int line = 1;
        for (int b = 0; b < blocks.size(); ++b) {
            final Block block = blocks.get(b);
            final int bl = block.getLine();
            while(line < bl) {
                strb.append("//\n");
                line += 1;
            }
            if (block.getType() == BlockType.VERBATIM) {
                strb.append("jexl:print(");
                strb.append(nuexpr++);
                strb.append(");\n");
                line += 1;
            } else {
                // keep track of first block of code, the frame creator
                if (codeStart < 0) {
                    codeStart = b;
                }
                final String body = block.getBody();
                strb.append(body);
                for(int c = 0; c < body.length(); ++c) {
                    if (body.charAt(c) == '\n') {
                        line += 1;
                    }
                }
            }
        }
        // create the script
        if (info == null) {
            info = jxlt.getEngine().createInfo();
        }
        // allow lambda defining params
        final Scope scope = parms == null ? null : new Scope(null, parms);
        script = jxlt.getEngine().parse(info.at(1, 1), false, strb.toString(), scope).script();
        // seek the map of expression number to scope so we can parse Unified
        // expression blocks with the appropriate symbols
        final Map<Integer, JexlNode.Info> minfo = new TreeMap<>();
        collectPrintScope(script.script(), minfo);
        // jexl:print(...) expression counter
        int jpe = 0;
        // create the exprs using the intended scopes
        for (final Block block : blocks) {
            if (block.getType() == BlockType.VERBATIM) {
                final JexlNode.Info ji = minfo.get(jpe);
                TemplateExpression te;
                // no node info means this verbatim is surrounded by comments markers;
                // expr at this index is never called
                if (ji != null) {
                    te = jxlt.parseExpression(ji, block.getBody(), scopeOf(ji));
                } else {
                    te = jxlt.new ConstantExpression(block.getBody(), null);
                }
                uexprs.add(te);
                jpe += 1;
            }
        }
        source = blocks.toArray(new Block[blocks.size()]);
        exprs = uexprs.toArray(new TemplateExpression[uexprs.size()]);
    }

    /**
     * Private ctor used to expand deferred expressions during prepare.
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

    /**
     * Gets the scope from an info.
     * @param info the node info
     * @return the scope
     */
    private static Scope scopeOf(final JexlNode.Info info) {
        JexlNode walk = info.getNode();
        while(walk != null) {
            if (walk instanceof ASTJexlScript) {
                return ((ASTJexlScript) walk).getScope();
            }
            walk = walk.jjtGetParent();
        }
        return null;
    }

    /**
     * Collects the scope surrounding a call to jexl:print(i).
     * <p>This allows to later parse the blocks with the known symbols
     * in the frame visible to the parser.
     * @param node the visited node
     * @param minfo the map of printed expression number to node info
     */
    private static void collectPrintScope(final JexlNode node, final Map<Integer, JexlNode.Info> minfo) {
        final int nc = node.jjtGetNumChildren();
        if (node instanceof ASTFunctionNode && nc == 2) {
            // 0 must be the prefix jexl:
            final ASTIdentifier nameNode = (ASTIdentifier) node.jjtGetChild(0);
            if ("print".equals(nameNode.getName()) && "jexl".equals(nameNode.getNamespace())) {
                final ASTArguments argNode = (ASTArguments) node.jjtGetChild(1);
                if (argNode.jjtGetNumChildren() == 1) {
                    // seek the epression number
                    final JexlNode arg0 = argNode.jjtGetChild(0);
                    if (arg0 instanceof ASTNumberLiteral) {
                        final int exprNumber = ((ASTNumberLiteral) arg0).getLiteral().intValue();
                        minfo.put(exprNumber, new JexlNode.Info(nameNode));
                        return;
                    }
                }
            }
        }
        for (int c = 0; c < nc; ++c) {
            collectPrintScope(node.jjtGetChild(c), minfo);
        }
    }

    /**
     * @return script
     */
    ASTJexlScript getScript() {
        return script;
    }

    /**
     * @return exprs
     */
    TemplateExpression[] getExpressions() {
        return exprs;
    }

    @Override
    public String toString() {
        final StringBuilder strb = new StringBuilder();
        for (final Block block : source) {
            block.toString(strb, prefix);
        }
        return strb.toString();
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
    public TemplateScript prepare(final JexlContext context) {
        final Engine jexl = jxlt.getEngine();
        final JexlOptions options = jexl.options(script, context);
        final Frame frame = script.createFrame((Object[]) null);
        final TemplateInterpreter.Arguments targs = new TemplateInterpreter
                .Arguments(jxlt.getEngine())
                .context(context)
                .options(options)
                .frame(frame);
        final Interpreter interpreter = new TemplateInterpreter(targs);
        final TemplateExpression[] immediates = new TemplateExpression[exprs.length];
        for (int e = 0; e < exprs.length; ++e) {
            try {
                immediates[e] = exprs[e].prepare(interpreter);
            } catch (final JexlException xjexl) {
                final JexlException xuel = TemplateEngine.createException(xjexl.getInfo(), "prepare", exprs[e], xjexl);
                if (jexl.isSilent()) {
                    jexl.logger.warn(xuel.getMessage(), xuel.getCause());
                    return null;
                }
                throw xuel;
            }
        }
        return new TemplateScript(jxlt, prefix, source, script, immediates);
    }

    @Override
    public void evaluate(final JexlContext context, final Writer writer) {
        evaluate(context, writer, (Object[]) null);
    }

    @Override
    public void evaluate(final JexlContext context, final Writer writer, final Object... args) {
        final Engine jexl = jxlt.getEngine();
        final JexlOptions options = jexl.options(script, context);
        final Frame frame = script.createFrame(args);
        final TemplateInterpreter.Arguments targs = new TemplateInterpreter
                .Arguments(jexl)
                .context(context)
                .options(options)
                .frame(frame)
                .expressions(exprs)
                .writer(writer);
        final Interpreter interpreter = new TemplateInterpreter(targs);
        interpreter.interpret(script);
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
    public String[] getParameters() {
        return script.getParameters();
    }

    @Override
    public Map<String, Object> getPragmas() {
        return script.getPragmas();
    }
}
