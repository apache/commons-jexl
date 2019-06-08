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
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public TemplateScript(TemplateEngine engine, JexlInfo info, String directive, Reader reader, String... parms) {
        if (directive == null) {
            throw new NullPointerException("null prefix");
        }
        if (Character.toString(engine.getImmediateChar()).equals(directive)
            || (Character.toString(engine.getImmediateChar()) + "{").equals(directive)
            || Character.toString(engine.getDeferredChar()).equals(directive)
            || (Character.toString(engine.getDeferredChar()) + "{").equals(directive)) {
            throw new IllegalArgumentException(directive + ": is not a valid directive pattern");
        }
        if (reader == null) {
            throw new NullPointerException("null input");
        }
        this.jxlt = engine;
        Scope scope = parms == null ? null : new Scope(null, parms);
        prefix = directive;
        List<Block> blocks = jxlt.readTemplate(prefix, reader);
        List<TemplateExpression> uexprs = new ArrayList<TemplateExpression>();
        StringBuilder strb = new StringBuilder();
        int nuexpr = 0;
        int codeStart = -1;
        for (int b = 0; b < blocks.size(); ++b) {
            Block block = blocks.get(b);
            if (block.getType() == BlockType.VERBATIM) {
                strb.append("jexl:print(");
                strb.append(nuexpr++);
                strb.append(");\n");
            } else {
                // keep track of first block of code, the frame creator
                if (codeStart < 0) {
                    codeStart = b;
                }
                strb.append(block.getBody());
            }
        }
        // create the script
        if (info == null) {
            info = jxlt.getEngine().createInfo();
        }
        // allow lambda defining params
        script = jxlt.getEngine().parse(info.at(0, 0), false, strb.toString(), scope).script();
        scope = script.getScope();
        // create the exprs using the code frame for those appearing after the first block of code
        for (int b = 0; b < blocks.size(); ++b) {
            Block block = blocks.get(b);
            if (block.getType() == BlockType.VERBATIM) {
                uexprs.add(
                        jxlt.parseExpression(
                                info.at(block.getLine(), 0),
                                block.getBody(),
                                b > codeStart ? scope : null)
                );
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
    TemplateScript(TemplateEngine engine,
                   String thePrefix,
                   Block[] theSource,
                   ASTJexlScript theScript,
                   TemplateExpression[] theExprs) {
        jxlt = engine;
        prefix = thePrefix;
        source = theSource;
        script = theScript;
        exprs = theExprs;
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
        StringBuilder strb = new StringBuilder();
        for (Block block : source) {
            block.toString(strb, prefix);
        }
        return strb.toString();
    }

    @Override
    public String asString() {
        StringBuilder strb = new StringBuilder();
        int e = 0;
        for (Block block : source) {
            if (block.getType() == BlockType.DIRECTIVE) {
                strb.append(prefix);
            } else {
                exprs[e++].asString(strb);
            }
        }
        return strb.toString();
    }

    @Override
    public TemplateScript prepare(JexlContext context) {
        Scope.Frame frame = script.createFrame((Object[]) null);
        TemplateExpression[] immediates = new TemplateExpression[exprs.length];
        for (int e = 0; e < exprs.length; ++e) {
            immediates[e] = exprs[e].prepare(frame, context);
        }
        return new TemplateScript(jxlt, prefix, source, script, immediates);
    }

    @Override
    public void evaluate(JexlContext context, Writer writer) {
        evaluate(context, writer, (Object[]) null);
    }

    @Override
    public void evaluate(JexlContext context, Writer writer, Object... args) {
        Scope.Frame frame = script.createFrame(args);
        Interpreter interpreter = new TemplateInterpreter(jxlt.getEngine(), context, frame, exprs, writer);
        interpreter.interpret(script);
    }

    @Override
    public Set<List<String>> getVariables() {
        Engine.VarCollector collector = jxlt.getEngine().varCollector();
        for (TemplateExpression expr : exprs) {
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
