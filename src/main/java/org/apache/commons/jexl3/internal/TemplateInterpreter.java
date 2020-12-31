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
import org.apache.commons.jexl3.JexlOptions;
import org.apache.commons.jexl3.internal.TemplateEngine.TemplateExpression;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTJexlLambda;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.JexlNode;

import java.io.Writer;

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
     * Helper ctor.
     * <p>Stores the different properties required to create a Template interpreter.
     */
    static class Arguments {
        /** The engine. */
        final Engine jexl;
        /** The options. */
        JexlOptions options;
        /** The context. */
        JexlContext jcontext;
        /** The frame. */
        Frame jframe;
        /** The expressions. */
        TemplateExpression[] expressions;
        /** The writer. */
        Writer out;

        /**
         * Sole ctor.
         * @param e the JEXL engine
         */
        Arguments(final Engine e) {
            this.jexl = e;
        }
        /**
         * Sets the options.
         * @param o the options
         * @return this instance
         */
        Arguments options(final JexlOptions o) {
            this.options = o;
            return this;
        }
        /**
         * Sets the context.
         * @param j the context
         * @return this instance
         */
        Arguments context(final JexlContext j) {
            this.jcontext = j;
            return this;
        }
        /**
         * Sets the frame.
         * @param f the frame
         * @return this instance
         */
        Arguments frame(final Frame f) {
            this.jframe = f;
            return this;
        }
        /**
         * Sets the expressions.
         * @param e the expressions
         * @return this instance
         */
        Arguments expressions(final TemplateExpression[] e) {
            this.expressions = e;
            return this;
        }
        /**
         * Sets the writer.
         * @param o the writer
         * @return this instance
         */
        Arguments writer(final Writer o) {
            this.out = o;
            return this;
        }
    }

    /**
     * Creates a template interpreter instance.
     * @param args the template interpreter arguments
     */
    TemplateInterpreter(final Arguments args) {
        super(args.jexl, args.options, args.jcontext, args.jframe);
        exprs = args.expressions;
        writer = args.out;
        block = new LexicalFrame(frame, null);
    }

    /**
     * Includes a call to another template.
     * <p>
     * Includes another template using this template initial context and writer.</p>
     * @param script the TemplateScript to evaluate
     * @param args   the arguments
     */
    public void include(final TemplateScript script, final Object... args) {
        script.evaluate(context, writer, args);
    }

    /**
     * Prints a unified expression evaluation result.
     * @param e the expression number
     */
    public void print(final int e) {
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
    private void printComposite(final TemplateEngine.CompositeExpression composite) {
        final TemplateEngine.TemplateExpression[] cexprs = composite.exprs;
        Object value;
        for (final TemplateExpression cexpr : cexprs) {
            value = cexpr.evaluate(this);
            doPrint(cexpr.getInfo(), value);
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
    private void doPrint(final JexlInfo info, final Object arg) {
        try {
            if (writer != null) {
                if (arg instanceof CharSequence) {
                    writer.write(arg.toString());
                } else if (arg != null) {
                    final Object[] value = {arg};
                    final JexlUberspect uber = jexl.getUberspect();
                    final JexlMethod method = uber.getMethod(writer, "print", value);
                    if (method != null) {
                        method.invoke(writer, value);
                    } else {
                        writer.write(arg.toString());
                    }
                }
            }
        } catch (final java.io.IOException xio) {
            throw TemplateEngine.createException(info, "call print", null, xio);
        } catch (final java.lang.Exception xany) {
            throw TemplateEngine.createException(info, "invoke print", null, xany);
        }
    }

    @Override
    protected Object resolveNamespace(final String prefix, final JexlNode node) {
        return "jexl".equals(prefix)? this : super.resolveNamespace(prefix, node);
    }

    @Override
    protected Object visit(final ASTIdentifier node, final Object data) {
        final String name = node.getName();
        if ("$jexl".equals(name)) {
            return writer;
        }
        return super.visit(node, data);
    }

    @Override
    protected Object visit(final ASTJexlScript script, final Object data) {
        if (script instanceof ASTJexlLambda && !((ASTJexlLambda) script).isTopLevel()) {
            return new Closure(this, (ASTJexlLambda) script) {
                @Override
                protected Interpreter createInterpreter(final JexlContext context, final Frame local) {
                    final JexlOptions opts = jexl.options(script, context);
                    final TemplateInterpreter.Arguments targs = new TemplateInterpreter.Arguments(jexl)
                            .context(context)
                            .options(opts)
                            .frame(local)
                            .expressions(exprs)
                            .writer(writer);
                    return new TemplateInterpreter(targs);
                }
            };
        }
        // otherwise...
        final int numChildren = script.jjtGetNumChildren();
            Object result = null;
            for (int i = 0; i < numChildren; i++) {
            final JexlNode child = script.jjtGetChild(i);
                result = child.jjtAccept(this, data);
                cancelCheck(child);
            }
            return result;
        }

}
