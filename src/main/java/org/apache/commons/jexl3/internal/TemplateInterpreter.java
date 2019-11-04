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
import org.apache.commons.jexl3.internal.TemplateEngine.TemplateExpression;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.JexlNode;
import java.io.Writer;
import org.apache.commons.jexl3.parser.ASTJexlLambda;
import org.apache.commons.jexl3.parser.ASTJexlScript;

/**
 * The type of interpreter to use during evaluation of templates.
 * <p>This context exposes its writer as '$jexl' to the scripts.</p>
 * <p>public for introspection purpose.</p>
 */
public class TemplateInterpreter extends Interpreter {
    /** The array of template expressions. */
    private final TemplateExpression[] exprs;
    /** The writer used to output. */
    private final Writer writer;

    /**
     * Creates a template interpreter instance.
     * @param jexl        the engine instance
     * @param jcontext    the base context
     * @param jframe      the calling frame
     * @param expressions the list of TemplateExpression from the TemplateScript to evaluate
     * @param out         the output writer
     */
    TemplateInterpreter(Engine jexl,
            JexlContext jcontext, Frame jframe, TemplateExpression[] expressions, Writer out) {
        super(jexl, jcontext, jframe);
        exprs = expressions;
        writer = out;
    }

    /**
     * Includes a call to another template.
     * <p>
     * Includes another template using this template initial context and writer.</p>
     * @param script the TemplateScript to evaluate
     * @param args   the arguments
     */
    public void include(TemplateScript script, Object... args) {
        script.evaluate(context, writer, args);
    }

    /**
     * Prints a unified expression evaluation result.
     * @param e the expression number
     */
    public void print(int e) {
        if (e < 0 || e >= exprs.length) {
            return;
        }
        TemplateEngine.TemplateExpression expr = exprs[e];
        if (expr.isDeferred()) {
            expr = expr.prepare(frame, context);
        }
        if (expr instanceof TemplateEngine.CompositeExpression) {
            printComposite((TemplateEngine.CompositeExpression) expr);
        } else {
            doPrint(expr.getInfo(), expr.evaluate(this));
        }
    }

    /**
     * Prints a composite expression.
     * @param composite the composite expression
     */
    private void printComposite(TemplateEngine.CompositeExpression composite) {
        TemplateEngine.TemplateExpression[] cexprs = composite.exprs;
        final int size = cexprs.length;
        Object value;
        for (int e = 0; e < size; ++e) {
            value = cexprs[e].evaluate(this);
            doPrint(cexprs[e].getInfo(), value);
        }
    }

    /**
     * Prints to output.
     * <p>
     * This will dynamically try to find the best suitable method in the writer through uberspection.
     * Subclassing Writer by adding 'print' methods should be the preferred way to specialize output.
     * </p>
     * @param info the source info
     * @param arg  the argument to print out
     */
    private void doPrint(JexlInfo info, Object arg) {
        try {
            if (writer != null) {
                if (arg instanceof CharSequence) {
                    writer.write(arg.toString());
                } else if (arg != null) {
                    Object[] value = {arg};
                    JexlUberspect uber = jexl.getUberspect();
                    JexlMethod method = uber.getMethod(writer, "print", value);
                    if (method != null) {
                        method.invoke(writer, value);
                    } else {
                        writer.write(arg.toString());
                    }
                }
            }
        } catch (java.io.IOException xio) {
            throw TemplateEngine.createException(info, "call print", null, xio);
        } catch (java.lang.Exception xany) {
            throw TemplateEngine.createException(info, "invoke print", null, xany);
        }
    }

    @Override
    protected Object resolveNamespace(String prefix, JexlNode node) {
        return "jexl".equals(prefix)? this : super.resolveNamespace(prefix, node);
    }

    @Override
    protected Object visit(ASTIdentifier node, Object data) {
        String name = node.getName();
        if ("$jexl".equals(name)) {
            return writer;
        }
        return super.visit(node, data);
    }

    @Override
    protected Object visit(ASTJexlScript node, Object data) {
        if (node instanceof ASTJexlLambda && !((ASTJexlLambda) node).isTopLevel()) {
            return new Closure(this, (ASTJexlLambda) node) {
                @Override
                protected Interpreter createInterpreter(JexlContext context, Frame local) {
                    return new TemplateInterpreter(jexl, context, local, exprs, writer);
                }
            };
        }
        block = new LexicalFrame(frame, block).declareArgs();
        try {
            // otherwise...
            final int numChildren = node.jjtGetNumChildren();
            Object result = null;
            for (int i = 0; i < numChildren; i++) {
                JexlNode child = node.jjtGetChild(i);
                result = child.jjtAccept(this, data);
                cancelCheck(child);
            }
            return result;
        } finally {
            block = block.pop();
        }
    }

}
