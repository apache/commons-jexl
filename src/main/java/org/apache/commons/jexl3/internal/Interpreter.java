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
//CSOFF: FileLength
package org.apache.commons.jexl3.internal;

import java.util.Iterator;
import java.util.concurrent.Callable;

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.JexlOperator;
import org.apache.commons.jexl3.JexlOptions;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.JxltEngine;

import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;

import org.apache.commons.jexl3.parser.ASTAddNode;
import org.apache.commons.jexl3.parser.ASTAndNode;
import org.apache.commons.jexl3.parser.ASTAnnotatedStatement;
import org.apache.commons.jexl3.parser.ASTAnnotation;
import org.apache.commons.jexl3.parser.ASTArguments;
import org.apache.commons.jexl3.parser.ASTArrayAccess;
import org.apache.commons.jexl3.parser.ASTArrayLiteral;
import org.apache.commons.jexl3.parser.ASTAssignment;
import org.apache.commons.jexl3.parser.ASTBitwiseAndNode;
import org.apache.commons.jexl3.parser.ASTBitwiseComplNode;
import org.apache.commons.jexl3.parser.ASTBitwiseOrNode;
import org.apache.commons.jexl3.parser.ASTBitwiseXorNode;
import org.apache.commons.jexl3.parser.ASTBlock;
import org.apache.commons.jexl3.parser.ASTBreak;
import org.apache.commons.jexl3.parser.ASTConstructorNode;
import org.apache.commons.jexl3.parser.ASTContinue;
import org.apache.commons.jexl3.parser.ASTDivNode;
import org.apache.commons.jexl3.parser.ASTDoWhileStatement;
import org.apache.commons.jexl3.parser.ASTEQNode;
import org.apache.commons.jexl3.parser.ASTERNode;
import org.apache.commons.jexl3.parser.ASTEWNode;
import org.apache.commons.jexl3.parser.ASTEmptyFunction;
import org.apache.commons.jexl3.parser.ASTExtendedLiteral;
import org.apache.commons.jexl3.parser.ASTFalseNode;
import org.apache.commons.jexl3.parser.ASTForeachStatement;
import org.apache.commons.jexl3.parser.ASTFunctionNode;
import org.apache.commons.jexl3.parser.ASTGENode;
import org.apache.commons.jexl3.parser.ASTGTNode;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTIdentifierAccess;
import org.apache.commons.jexl3.parser.ASTIdentifierAccessJxlt;
import org.apache.commons.jexl3.parser.ASTIfStatement;
import org.apache.commons.jexl3.parser.ASTJexlLambda;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.ASTJxltLiteral;
import org.apache.commons.jexl3.parser.ASTLENode;
import org.apache.commons.jexl3.parser.ASTLTNode;
import org.apache.commons.jexl3.parser.ASTMapEntry;
import org.apache.commons.jexl3.parser.ASTMapLiteral;
import org.apache.commons.jexl3.parser.ASTMethodNode;
import org.apache.commons.jexl3.parser.ASTModNode;
import org.apache.commons.jexl3.parser.ASTMulNode;
import org.apache.commons.jexl3.parser.ASTNENode;
import org.apache.commons.jexl3.parser.ASTNEWNode;
import org.apache.commons.jexl3.parser.ASTNRNode;
import org.apache.commons.jexl3.parser.ASTNSWNode;
import org.apache.commons.jexl3.parser.ASTNotNode;
import org.apache.commons.jexl3.parser.ASTNullLiteral;
import org.apache.commons.jexl3.parser.ASTNullpNode;
import org.apache.commons.jexl3.parser.ASTNumberLiteral;
import org.apache.commons.jexl3.parser.ASTOrNode;
import org.apache.commons.jexl3.parser.ASTRangeNode;
import org.apache.commons.jexl3.parser.ASTReference;
import org.apache.commons.jexl3.parser.ASTReferenceExpression;
import org.apache.commons.jexl3.parser.ASTRegexLiteral;
import org.apache.commons.jexl3.parser.ASTReturnStatement;
import org.apache.commons.jexl3.parser.ASTSWNode;
import org.apache.commons.jexl3.parser.ASTSetAddNode;
import org.apache.commons.jexl3.parser.ASTSetAndNode;
import org.apache.commons.jexl3.parser.ASTSetDivNode;
import org.apache.commons.jexl3.parser.ASTSetLiteral;
import org.apache.commons.jexl3.parser.ASTSetModNode;
import org.apache.commons.jexl3.parser.ASTSetMultNode;
import org.apache.commons.jexl3.parser.ASTSetOrNode;
import org.apache.commons.jexl3.parser.ASTSetSubNode;
import org.apache.commons.jexl3.parser.ASTSetXorNode;
import org.apache.commons.jexl3.parser.ASTSizeFunction;
import org.apache.commons.jexl3.parser.ASTStringLiteral;
import org.apache.commons.jexl3.parser.ASTSubNode;
import org.apache.commons.jexl3.parser.ASTTernaryNode;
import org.apache.commons.jexl3.parser.ASTTrueNode;
import org.apache.commons.jexl3.parser.ASTUnaryMinusNode;
import org.apache.commons.jexl3.parser.ASTUnaryPlusNode;
import org.apache.commons.jexl3.parser.ASTVar;
import org.apache.commons.jexl3.parser.ASTWhileStatement;
import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.Node;

/**
 * An interpreter of JEXL syntax.
 *
 * @since 2.0
 */
public class Interpreter extends InterpreterBase {
    /** Frame height. */
    protected int fp = 0;
    /** Symbol values. */
    protected final Frame frame;
    /** Block micro-frames. */
    protected LexicalFrame block = null;
    
    /**
     * The thread local interpreter.
     */
    protected static final java.lang.ThreadLocal<Interpreter> INTER =
                       new java.lang.ThreadLocal<Interpreter>();

    /**
     * Creates an interpreter.
     * @param engine   the engine creating this interpreter
     * @param aContext the evaluation context, global variables, methods and functions
     * @param opts     the evaluation options, flags modifying evaluation behavior
     * @param eFrame   the evaluation frame, arguments and local variables
     */
    protected Interpreter(final Engine engine, final JexlOptions opts, final JexlContext aContext, final Frame eFrame) {
        super(engine, opts, aContext);
        this.frame = eFrame;
    }

    /**
     * Copy constructor.
     * @param ii  the interpreter to copy
     * @param jexla the arithmetic instance to use (or null)
     */
    protected Interpreter(final Interpreter ii, final JexlArithmetic jexla) {
        super(ii, jexla);
        frame = ii.frame;
        block = ii.block != null? new LexicalFrame(ii.block) : null;
    }

    /**
     * Swaps the current thread local interpreter.
     * @param inter the interpreter or null
     * @return the previous thread local interpreter
     */
    protected Interpreter putThreadInterpreter(final Interpreter inter) {
        final Interpreter pinter = INTER.get();
        INTER.set(inter);
        return pinter;
    }

    /**
     * Interpret the given script/expression.
     * <p>
     * If the underlying JEXL engine is silent, errors will be logged through
     * its logger as warning.
     * @param node the script or expression to interpret.
     * @return the result of the interpretation.
     * @throws JexlException if any error occurs during interpretation.
     */
    public Object interpret(final JexlNode node) {
        JexlContext.ThreadLocal tcontext = null;
        JexlEngine tjexl = null;
        Interpreter tinter = null;
        try {
            tinter = putThreadInterpreter(this);
            if (tinter != null) {
                fp = tinter.fp + 1;
            }
            if (context instanceof JexlContext.ThreadLocal) {
                tcontext = jexl.putThreadLocal((JexlContext.ThreadLocal) context);
            }
            tjexl = jexl.putThreadEngine(jexl);
            if (fp > jexl.stackOverflow) {
                throw new JexlException.StackOverflow(node.jexlInfo(), "jexl (" + jexl.stackOverflow + ")", null);
            }
            cancelCheck(node);
            return node.jjtAccept(this, null);
        } catch(final StackOverflowError xstack) {
            final JexlException xjexl = new JexlException.StackOverflow(node.jexlInfo(), "jvm", xstack);
            if (!isSilent()) {
                throw xjexl.clean();
            }
            if (logger.isWarnEnabled()) {
                logger.warn(xjexl.getMessage(), xjexl.getCause());
            }
        } catch (final JexlException.Return xreturn) {
            return xreturn.getValue();
        } catch (final JexlException.Cancel xcancel) {
            // cancelled |= Thread.interrupted();
            cancelled.weakCompareAndSet(false, Thread.interrupted());
            if (isCancellable()) {
                throw xcancel.clean();
            }
        } catch (final JexlException xjexl) {
            if (!isSilent()) {
                throw xjexl.clean();
            }
            if (logger.isWarnEnabled()) {
                logger.warn(xjexl.getMessage(), xjexl.getCause());
            }
        } finally {
            synchronized(this) {
                if (functors != null) {
                    for (final Object functor : functors.values()) {
                        closeIfSupported(functor);
                    }
                    functors.clear();
                    functors = null;
                }
            }
            jexl.putThreadEngine(tjexl);
            if (context instanceof JexlContext.ThreadLocal) {
                jexl.putThreadLocal(tcontext);
            }
            if (tinter != null) {
                fp = tinter.fp - 1;
            }
            putThreadInterpreter(tinter);
        }
        return null;
    }

    /**
     * Gets an attribute of an object.
     *
     * @param object    to retrieve value from
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or key for a map
     * @return the attribute value
     */
    public Object getAttribute(final Object object, final Object attribute) {
        return getAttribute(object, attribute, null);
    }
    /**
     * Sets an attribute of an object.
     *
     * @param object    to set the value to
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or key for a map
     * @param value     the value to assign to the object's attribute
     */
    public void setAttribute(final Object object, final Object attribute, final Object value) {
        setAttribute(object, attribute, value, null);
    }

    @Override
    protected Object visit(final ASTAddNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.ADD, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.add(left, right);
        } catch (final ArithmeticException xrt) {
            throw new JexlException(node, "+ error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTSubNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.SUBTRACT, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.subtract(left, right);
        } catch (final ArithmeticException xrt) {
            throw new JexlException(node, "- error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTMulNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.MULTIPLY, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.multiply(left, right);
        } catch (final ArithmeticException xrt) {
            final JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "* error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTDivNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.DIVIDE, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.divide(left, right);
        } catch (final ArithmeticException xrt) {
            if (!arithmetic.isStrict()) {
                return 0.0d;
            }
            final JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "/ error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTModNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.MOD, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.mod(left, right);
        } catch (final ArithmeticException xrt) {
            if (!arithmetic.isStrict()) {
                return 0.0d;
            }
            final JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "% error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTBitwiseAndNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.AND, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.and(left, right);
        } catch (final ArithmeticException xrt) {
            throw new JexlException(node, "& error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTBitwiseOrNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.OR, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.or(left, right);
        } catch (final ArithmeticException xrt) {
            throw new JexlException(node, "| error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTBitwiseXorNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.XOR, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.xor(left, right);
        } catch (final ArithmeticException xrt) {
            throw new JexlException(node, "^ error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTEQNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.EQ, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? result
                   : arithmetic.equals(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (final ArithmeticException xrt) {
            throw new JexlException(node, "== error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTNENode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.EQ, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? arithmetic.toBoolean(result) ? Boolean.FALSE : Boolean.TRUE
                   : arithmetic.equals(left, right) ? Boolean.FALSE : Boolean.TRUE;
        } catch (final ArithmeticException xrt) {
            final JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "!= error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTGENode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.GTE, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? result
                   : arithmetic.greaterThanOrEqual(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (final ArithmeticException xrt) {
            throw new JexlException(node, ">= error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTGTNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.GT, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? result
                   : arithmetic.greaterThan(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (final ArithmeticException xrt) {
            throw new JexlException(node, "> error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTLENode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.LTE, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? result
                   : arithmetic.lessThanOrEqual(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (final ArithmeticException xrt) {
            throw new JexlException(node, "<= error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTLTNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.LT, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? result
                   : arithmetic.lessThan(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (final ArithmeticException xrt) {
            throw new JexlException(node, "< error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTSWNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return operators.startsWith(node, "^=", left, right) ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    protected Object visit(final ASTNSWNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return operators.startsWith(node, "^!", left, right) ? Boolean.FALSE : Boolean.TRUE;
    }

    @Override
    protected Object visit(final ASTEWNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return operators.endsWith(node, "$=", left, right) ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    protected Object visit(final ASTNEWNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return operators.endsWith(node, "$!", left, right) ? Boolean.FALSE : Boolean.TRUE;
    }

    @Override
    protected Object visit(final ASTERNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return operators.contains(node, "=~", right, left) ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    protected Object visit(final ASTNRNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return operators.contains(node, "!~", right, left) ? Boolean.FALSE : Boolean.TRUE;
    }

    @Override
    protected Object visit(final ASTRangeNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.createRange(left, right);
        } catch (final ArithmeticException xrt) {
            final JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, ".. error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTUnaryMinusNode node, final Object data) {
        // use cached value if literal
        final Object value = node.jjtGetValue();
        if (value != null && !(value instanceof JexlMethod)) {
            return value;
        }
        final JexlNode valNode = node.jjtGetChild(0);
        final Object val = valNode.jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.NEGATE, val);
            if (result != JexlEngine.TRY_FAILED) {
                return result;
            }
            Object number = arithmetic.negate(val);
            // attempt to recoerce to literal class
            if ((number instanceof Number)) {
                // cache if number literal and negate is idempotent
                if (valNode instanceof ASTNumberLiteral) {
                    number = arithmetic.narrowNumber((Number) number, ((ASTNumberLiteral) valNode).getLiteralClass());
                    if (arithmetic.isNegateStable()) {
                        node.jjtSetValue(number);
                    }
                }
            }
            return number;
        } catch (final ArithmeticException xrt) {
            throw new JexlException(valNode, "- error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTUnaryPlusNode node, final Object data) {
        // use cached value if literal
        final Object value = node.jjtGetValue();
        if (value != null && !(value instanceof JexlMethod)) {
            return value;
        }
        final JexlNode valNode = node.jjtGetChild(0);
        final Object val = valNode.jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.POSITIVIZE, val);
            if (result != JexlEngine.TRY_FAILED) {
                return result;
            }
            final Object number = arithmetic.positivize(val);
            if (valNode instanceof ASTNumberLiteral
                && number instanceof Number
                && arithmetic.isPositivizeStable()) {
                node.jjtSetValue(number);
            }
            return number;
        } catch (final ArithmeticException xrt) {
            throw new JexlException(valNode, "- error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTBitwiseComplNode node, final Object data) {
        final Object arg = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.COMPLEMENT, arg);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.complement(arg);
        } catch (final ArithmeticException xrt) {
            throw new JexlException(node, "~ error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTNotNode node, final Object data) {
        final Object val = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            final Object result = operators.tryOverload(node, JexlOperator.NOT, val);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.not(val);
        } catch (final ArithmeticException xrt) {
            throw new JexlException(node, "! error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTIfStatement node, final Object data) {
        final int n = 0;
        final int numChildren = node.jjtGetNumChildren();
        try {
            Object result = null;
            // pairs of { conditions , 'then' statement }
            for(int ifElse = 0; ifElse < (numChildren - 1); ifElse += 2) {
                final Object condition = node.jjtGetChild(ifElse).jjtAccept(this, null);
                if (arithmetic.toBoolean(condition)) {
                    // first objectNode is true statement
                    return node.jjtGetChild(ifElse + 1).jjtAccept(this, null);
                }
            }
            // if odd...
            if ((numChildren & 1) == 1) {
                // if there is an else, there are an odd number of children in the statement and it is the last child,
                // execute it.
                result = node.jjtGetChild(numChildren - 1).jjtAccept(this, null);
            }
            return result;
        } catch (final ArithmeticException xrt) {
            throw new JexlException(node.jjtGetChild(n), "if error", xrt);
        }
    }

    @Override
    protected Object visit(final ASTVar node, final Object data) {
        final int symbol = node.getSymbol();
        // if we have a var, we have a scope thus a frame
        if (!options.isLexical()) {
            if (frame.has(symbol)) {
                return frame.get(symbol);
            }
        } else if (!defineVariable(node, block)) {
            return redefinedVariable(node, node.getName());
        }
        frame.set(symbol, null);
        return null;
    }

    @Override
    protected Object visit(final ASTBlock node, final Object data) {
        final int cnt = node.getSymbolCount();
        if (!options.isLexical() || cnt <= 0) {
            return visitBlock(node, data);
        }
        try {
            block = new LexicalFrame(frame, block);
            return visitBlock(node, data);
        } finally {
            block = block.pop();
        }
    }

    /**
     * Base visitation for blocks.
     * @param node the block
     * @param data the usual data
     * @return the result of the last expression evaluation
     */
    private Object visitBlock(final ASTBlock node, final Object data) {
        final int numChildren = node.jjtGetNumChildren();
        Object result = null;
        for (int i = 0; i < numChildren; i++) {
            cancelCheck(node);
            result = node.jjtGetChild(i).jjtAccept(this, data);
        }
        return result;
    }

    @Override
    protected Object visit(final ASTReturnStatement node, final Object data) {
        final Object val = node.jjtGetChild(0).jjtAccept(this, data);
        cancelCheck(node);
        throw new JexlException.Return(node, null, val);
    }

    @Override
    protected Object visit(final ASTContinue node, final Object data) {
        throw new JexlException.Continue(node);
    }

    @Override
    protected Object visit(final ASTBreak node, final Object data) {
        throw new JexlException.Break(node);
    }

    @Override
    protected Object visit(final ASTForeachStatement node, final Object data) {
        Object result = null;
        /* first objectNode is the loop variable */
        final ASTReference loopReference = (ASTReference) node.jjtGetChild(0);
        final ASTIdentifier loopVariable = (ASTIdentifier) loopReference.jjtGetChild(0);
        final int symbol = loopVariable.getSymbol();
        final boolean lexical = options.isLexical();// && node.getSymbolCount() > 0;
        final LexicalFrame locals = lexical? new LexicalFrame(frame, block) : null;
        final boolean loopSymbol = symbol >= 0 && loopVariable instanceof ASTVar;
        if (lexical) {
            // create lexical frame
            // it may be a local previously declared
            if (loopSymbol && !defineVariable((ASTVar) loopVariable, locals)) {
                return redefinedVariable(node, loopVariable.getName());
            }
            block = locals;
        }
        Object forEach = null;
        try {
            /* second objectNode is the variable to iterate */
            final Object iterableValue = node.jjtGetChild(1).jjtAccept(this, data);
            // make sure there is a value to iterate upon
            if (iterableValue != null) {
                /* third objectNode is the statement to execute */
                final JexlNode statement = node.jjtGetNumChildren() >= 3 ? node.jjtGetChild(2) : null;
                // get an iterator for the collection/array etc via the introspector.
                forEach = operators.tryOverload(node, JexlOperator.FOR_EACH, iterableValue);
                final Iterator<?> itemsIterator = forEach instanceof Iterator
                        ? (Iterator<?>) forEach
                        : uberspect.getIterator(iterableValue);
                if (itemsIterator != null) {
                    int cnt = 0;
                    while (itemsIterator.hasNext()) {
                        cancelCheck(node);
                        // reset loop varaible
                        if (lexical && cnt++ > 0) {
                            // clean up but remain current
                            block.pop();
                            // unlikely to fail 
                            if (loopSymbol && !defineVariable((ASTVar) loopVariable, locals)) {
                                return redefinedVariable(node, loopVariable.getName());
                            }
                        }
                        // set loopVariable to value of iterator
                        final Object value = itemsIterator.next();
                        if (symbol < 0) {
                            setContextVariable(node, loopVariable.getName(), value);
                        } else {
                            frame.set(symbol, value);
                        }
                        if (statement != null) {
                            try {
                                // execute statement
                                result = statement.jjtAccept(this, data);
                            } catch (final JexlException.Break stmtBreak) {
                                break;
                            } catch (final JexlException.Continue stmtContinue) {
                                //continue;
                            }
                        }
                    }
                }
            }
        } finally {
            //  closeable iterator handling
            closeIfSupported(forEach);
            // restore lexical frame
            if (lexical) {
                block = block.pop();
            }
        }
        return result;
    }

    @Override
    protected Object visit(final ASTWhileStatement node, final Object data) {
        Object result = null;
        /* first objectNode is the condition */
        final Node condition = node.jjtGetChild(0);
        while (arithmetic.toBoolean(condition.jjtAccept(this, data))) {
            cancelCheck(node);
            if (node.jjtGetNumChildren() > 1) {
                try {
                    // execute statement
                    result = node.jjtGetChild(1).jjtAccept(this, data);
                } catch (final JexlException.Break stmtBreak) {
                    break;
                } catch (final JexlException.Continue stmtContinue) {
                    //continue;
                }
            }
        }
        return result;
    }

    @Override
    protected Object visit(final ASTDoWhileStatement node, final Object data) {
        Object result = null;
        final int nc = node.jjtGetNumChildren();
        /* last objectNode is the condition */
        final Node condition = node.jjtGetChild(nc - 1);
        do {
            cancelCheck(node);
            if (nc > 1) {
                try {
                    // execute statement
                    result = node.jjtGetChild(0).jjtAccept(this, data);
                } catch (final JexlException.Break stmtBreak) {
                    break;
                } catch (final JexlException.Continue stmtContinue) {
                    //continue;
                }
            }
        } while (arithmetic.toBoolean(condition.jjtAccept(this, data)));
        return result;
    }

    @Override
    protected Object visit(final ASTAndNode node, final Object data) {
        /*
         * The pattern for exception mgmt is to let the child*.jjtAccept out of the try/catch loop so that if one fails,
         * the ex will traverse up to the interpreter. In cases where this is not convenient/possible, JexlException
         * must be caught explicitly and rethrown.
         */
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            final boolean leftValue = arithmetic.toBoolean(left);
            if (!leftValue) {
                return Boolean.FALSE;
            }
        } catch (final ArithmeticException xrt) {
            throw new JexlException(node.jjtGetChild(0), "boolean coercion error", xrt);
        }
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final boolean rightValue = arithmetic.toBoolean(right);
            if (!rightValue) {
                return Boolean.FALSE;
            }
        } catch (final ArithmeticException xrt) {
            throw new JexlException(node.jjtGetChild(1), "boolean coercion error", xrt);
        }
        return Boolean.TRUE;
    }

    @Override
    protected Object visit(final ASTOrNode node, final Object data) {
        final Object left = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            final boolean leftValue = arithmetic.toBoolean(left);
            if (leftValue) {
                return Boolean.TRUE;
            }
        } catch (final ArithmeticException xrt) {
            throw new JexlException(node.jjtGetChild(0), "boolean coercion error", xrt);
        }
        final Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            final boolean rightValue = arithmetic.toBoolean(right);
            if (rightValue) {
                return Boolean.TRUE;
            }
        } catch (final ArithmeticException xrt) {
            throw new JexlException(node.jjtGetChild(1), "boolean coercion error", xrt);
        }
        return Boolean.FALSE;
    }

    @Override
    protected Object visit(final ASTNullLiteral node, final Object data) {
        return null;
    }

    @Override
    protected Object visit(final ASTTrueNode node, final Object data) {
        return Boolean.TRUE;
    }

    @Override
    protected Object visit(final ASTFalseNode node, final Object data) {
        return Boolean.FALSE;
    }

    @Override
    protected Object visit(final ASTNumberLiteral node, final Object data) {
        if (data != null && node.isInteger()) {
            return getAttribute(data, node.getLiteral(), node);
        }
        return node.getLiteral();
    }

    @Override
    protected Object visit(final ASTStringLiteral node, final Object data) {
        if (data != null) {
            return getAttribute(data, node.getLiteral(), node);
        }
        return node.getLiteral();
    }

    @Override
    protected Object visit(final ASTRegexLiteral node, final Object data) {
        return node.getLiteral();
    }

    @Override
    protected Object visit(final ASTArrayLiteral node, final Object data) {
        final int childCount = node.jjtGetNumChildren();
        final JexlArithmetic.ArrayBuilder ab = arithmetic.arrayBuilder(childCount);
        boolean extended = false;
        for (int i = 0; i < childCount; i++) {
            cancelCheck(node);
            final JexlNode child = node.jjtGetChild(i);
            if (child instanceof ASTExtendedLiteral) {
                extended = true;
            } else {
                final Object entry = node.jjtGetChild(i).jjtAccept(this, data);
                ab.add(entry);
            }
        }
        return ab.create(extended);
    }

    @Override
    protected Object visit(final ASTExtendedLiteral node, final Object data) {
        return node;
    }

    @Override
    protected Object visit(final ASTSetLiteral node, final Object data) {
        final int childCount = node.jjtGetNumChildren();
        final JexlArithmetic.SetBuilder mb = arithmetic.setBuilder(childCount);
        for (int i = 0; i < childCount; i++) {
            cancelCheck(node);
            final Object entry = node.jjtGetChild(i).jjtAccept(this, data);
            mb.add(entry);
        }
        return mb.create();
    }

    @Override
    protected Object visit(final ASTMapLiteral node, final Object data) {
        final int childCount = node.jjtGetNumChildren();
        final JexlArithmetic.MapBuilder mb = arithmetic.mapBuilder(childCount);
        for (int i = 0; i < childCount; i++) {
            cancelCheck(node);
            final Object[] entry = (Object[]) (node.jjtGetChild(i)).jjtAccept(this, data);
            mb.put(entry[0], entry[1]);
        }
        return mb.create();
    }

    @Override
    protected Object visit(final ASTMapEntry node, final Object data) {
        final Object key = node.jjtGetChild(0).jjtAccept(this, data);
        final Object value = node.jjtGetChild(1).jjtAccept(this, data);
        return new Object[]{key, value};
    }

    @Override
    protected Object visit(final ASTTernaryNode node, final Object data) {
        Object condition;
        try {
            condition = node.jjtGetChild(0).jjtAccept(this, data);
        } catch(final JexlException xany) {
            if (!(xany.getCause() instanceof JexlArithmetic.NullOperand)) {
                throw xany;
            }
            condition = null;
        }
        // ternary as in "x ? y : z"
        if (node.jjtGetNumChildren() == 3) {
            if (condition != null && arithmetic.toBoolean(condition)) {
                return node.jjtGetChild(1).jjtAccept(this, data);
            } else {
                return node.jjtGetChild(2).jjtAccept(this, data);
            }
        }
        // elvis as in "x ?: z"
        if (condition != null && arithmetic.toBoolean(condition)) {
            return condition;
        } else {
            return node.jjtGetChild(1).jjtAccept(this, data);
        }
    }

    @Override
    protected Object visit(final ASTNullpNode node, final Object data) {
        Object lhs;
        try {
            lhs = node.jjtGetChild(0).jjtAccept(this, data);
        } catch(final JexlException xany) {
            if (!(xany.getCause() instanceof JexlArithmetic.NullOperand)) {
                throw xany;
            }
            lhs = null;
        }
        // null elision as in "x ?? z"
        return lhs != null? lhs : node.jjtGetChild(1).jjtAccept(this, data);
    }

    @Override
    protected Object visit(final ASTSizeFunction node, final Object data) {
        try {
            final Object val = node.jjtGetChild(0).jjtAccept(this, data);
            return operators.size(node, val);
        } catch(final JexlException xany) {
            return 0;
        }
    }

    @Override
    protected Object visit(final ASTEmptyFunction node, final Object data) {
        try {
            final Object value = node.jjtGetChild(0).jjtAccept(this, data);
            return operators.empty(node, value);
        } catch(final JexlException xany) {
            return true;
        }
    }
    
    /**
     * Runs a node.
     * @param node the node
     * @param data the usual data
     * @return the return value
     */
    protected Object visitLexicalNode(final JexlNode node, final Object data) {
        block = new LexicalFrame(frame, null);
        try {
            return node.jjtAccept(this, data);
        } finally {
            block = block.pop();
        }
    }
    
    /**
     * Runs a closure.
     * @param closure the closure
     * @param data the usual data
     * @return the closure return value
     */
    protected Object runClosure(final Closure closure, final Object data) {
        final ASTJexlScript script = closure.getScript();
        block = new LexicalFrame(frame, block).defineArgs();
        try {
            final JexlNode body = script.jjtGetChild(script.jjtGetNumChildren() - 1);
            return interpret(body);
        } finally {
            block = block.pop();
        }
    }

    @Override
    protected Object visit(final ASTJexlScript script, final Object data) {
        if (script instanceof ASTJexlLambda && !((ASTJexlLambda) script).isTopLevel()) {
            return new Closure(this, (ASTJexlLambda) script);
        } else {
            block = new LexicalFrame(frame, block).defineArgs();
            try {
                final int numChildren = script.jjtGetNumChildren();
                Object result = null;
                for (int i = 0; i < numChildren; i++) {
                    final JexlNode child = script.jjtGetChild(i);
                    result = child.jjtAccept(this, data);
                    cancelCheck(child);
                }
                return result;
            } finally {
                block = block.pop();
            }
        }
    }

    @Override
    protected Object visit(final ASTReferenceExpression node, final Object data) {
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    protected Object visit(final ASTIdentifier identifier, final Object data) {
        cancelCheck(identifier);
        return data != null
                ? getAttribute(data, identifier.getName(), identifier)
                : getVariable(frame, block, identifier);
    }

    @Override
    protected Object visit(final ASTArrayAccess node, final Object data) {
        // first objectNode is the identifier
        Object object = data;
        // can have multiple nodes - either an expression, integer literal or reference
        final int numChildren = node.jjtGetNumChildren();
        for (int i = 0; i < numChildren; i++) {
            final JexlNode nindex = node.jjtGetChild(i);
            if (object == null) {
                return unsolvableProperty(nindex, stringifyProperty(nindex), false, null);
            }
            final Object index = nindex.jjtAccept(this, null);
            cancelCheck(node);
            object = getAttribute(object, index, nindex);
        }
        return object;
    }

    /**
     * Evaluates an access identifier based on the 2 main implementations;
     * static (name or numbered identifier) or dynamic (jxlt).
     * @param node the identifier access node
     * @return the evaluated identifier
     */
    private Object evalIdentifier(final ASTIdentifierAccess node) {
        if (node instanceof ASTIdentifierAccessJxlt) {
            final ASTIdentifierAccessJxlt accessJxlt = (ASTIdentifierAccessJxlt) node;
            final String src = node.getName();
            Throwable cause = null;
            TemplateEngine.TemplateExpression expr = (TemplateEngine.TemplateExpression) accessJxlt.getExpression();
            try {
                if (expr == null) {
                    final TemplateEngine jxlt = jexl.jxlt();
                    expr = jxlt.parseExpression(node.jexlInfo(), src, frame != null ? frame.getScope() : null);
                    accessJxlt.setExpression(expr);
                }
                if (expr != null) {
                    final Object name = expr.evaluate(frame, context);
                    if (name != null) {
                        final Integer id = ASTIdentifierAccess.parseIdentifier(name.toString());
                        return id != null ? id : name;
                    }
                }
            } catch (final JxltEngine.Exception xjxlt) {
                cause = xjxlt;
            }
            return node.isSafe() ? null : unsolvableProperty(node, src, true, cause);
        } else {
            return node.getIdentifier();
        }
    }

    @Override
    protected Object visit(final ASTIdentifierAccess node, final Object data) {
        if (data == null) {
            return null;
        }
        final Object id = evalIdentifier(node);
        return getAttribute(data, id, node);
    }

    @Override
    protected Object visit(final ASTReference node, final Object data) {
        cancelCheck(node);
        final int numChildren = node.jjtGetNumChildren();
        final JexlNode parent = node.jjtGetParent();
        // pass first piece of data in and loop through children
        Object object = null;
        JexlNode objectNode = null;
        JexlNode ptyNode = null;
        StringBuilder ant = null;
        boolean antish = !(parent instanceof ASTReference);
        int v = 1;
        main:
        for (int c = 0; c < numChildren; c++) {
            objectNode = node.jjtGetChild(c);
            if (objectNode instanceof ASTMethodNode) {
                antish = false;
                if (object == null) {
                    // we may be performing a method call on an antish var
                    if (ant != null) {
                        final JexlNode child = objectNode.jjtGetChild(0);
                        if (child instanceof ASTIdentifierAccess) {
                            final int alen = ant.length();
                            ant.append('.');
                            ant.append(((ASTIdentifierAccess) child).getName());
                            object = context.get(ant.toString());
                            if (object != null) {
                                object = visit((ASTMethodNode) objectNode, object, context);
                                continue;
                            } else {
                                // remove method name from antish
                                ant.delete(alen, ant.length());
                                ptyNode = objectNode;
                            }
                        }
                    }
                    break;
                }
            } else if (objectNode instanceof ASTArrayAccess) {
                antish = false;
                if (object == null) {
                    ptyNode = objectNode;
                    break;
                }
            }
            // attempt to evaluate the property within the object (visit(ASTIdentifierAccess node))
            object = objectNode.jjtAccept(this, object);
            cancelCheck(node);
            if (object != null) {
                // disallow mixing antish variable & bean with same root; avoid ambiguity
                antish = false;
            } else if (antish) {
                // create first from first node
                if (ant == null) {
                    // if we still have a null object, check for an antish variable
                    final JexlNode first = node.jjtGetChild(0);
                    if (first instanceof ASTIdentifier) {
                        final ASTIdentifier afirst = (ASTIdentifier) first;
                        ant = new StringBuilder(afirst.getName());
                        // skip the else...*
                    } else {
                        // not an identifier, not antish
                        ptyNode = objectNode;
                        break main;
                    }
                    // *... and continue
                    if (!options.isAntish()) {
                        antish = false;
                        continue;
                    }
                    // skip the first node case since it was trialed in jjtAccept above and returned null
                    if (c == 0) {
                        continue;
                    }
                }
                // catch up to current node
                for (; v <= c; ++v) {
                    final JexlNode child = node.jjtGetChild(v);
                    if (child instanceof ASTIdentifierAccess) {
                        final ASTIdentifierAccess achild = (ASTIdentifierAccess) child;
                        if (achild.isSafe() || achild.isExpression()) {
                            break main;
                        }
                        ant.append('.');
                        ant.append(achild.getName());
                    } else {
                        // not an identifier, not antish
                        ptyNode = objectNode;
                        break main;
                    }
                }
                // solve antish
                object = context.get(ant.toString());
            } else if (c != numChildren - 1) {
                // only the last one may be null
                ptyNode = objectNode;
                break; //
            }
        }
        // dealing with null
        if (object == null) {
            if (ptyNode != null) {
                if (ptyNode.isSafeLhs(isSafe())) {
                    return null;
                }
                if (ant != null) {
                    final String aname = ant.toString();
                    final boolean defined = isVariableDefined(frame, block, aname);
                    return unsolvableVariable(node, aname, !defined);
                }
                return unsolvableProperty(node,
                        stringifyProperty(ptyNode), ptyNode == objectNode, null);
            }
            if (antish) {
                if (node.isSafeLhs(isSafe())) {
                    return null;
                }
                final String aname = ant != null ? ant.toString() : "?";
                final boolean defined = isVariableDefined(frame, block, aname);
                if (defined && !arithmetic.isStrict()) {
                    return null;
                }
                if (!defined || !(node.jjtGetParent() instanceof ASTJexlScript)) {
                    return unsolvableVariable(node, aname, !defined);
                }
            }
        }
        return object;
    }

    @Override
    protected Object visit(final ASTAssignment node, final Object data) {
        return executeAssign(node, null, data);
    }

    @Override
    protected Object visit(final ASTSetAddNode node, final Object data) {
        return executeAssign(node, JexlOperator.SELF_ADD, data);
    }

    @Override
    protected Object visit(final ASTSetSubNode node, final Object data) {
        return executeAssign(node, JexlOperator.SELF_SUBTRACT, data);
    }

    @Override
    protected Object visit(final ASTSetMultNode node, final Object data) {
        return executeAssign(node, JexlOperator.SELF_MULTIPLY, data);
    }

    @Override
    protected Object visit(final ASTSetDivNode node, final Object data) {
        return executeAssign(node, JexlOperator.SELF_DIVIDE, data);
    }

    @Override
    protected Object visit(final ASTSetModNode node, final Object data) {
        return executeAssign(node, JexlOperator.SELF_MOD, data);
    }

    @Override
    protected Object visit(final ASTSetAndNode node, final Object data) {
        return executeAssign(node, JexlOperator.SELF_AND, data);
    }

    @Override
    protected Object visit(final ASTSetOrNode node, final Object data) {
        return executeAssign(node, JexlOperator.SELF_OR, data);
    }

    @Override
    protected Object visit(final ASTSetXorNode node, final Object data) {
        return executeAssign(node, JexlOperator.SELF_XOR, data);
    }

    /**
     * Executes an assignment with an optional side-effect operator.
     * @param node     the node
     * @param assignop the assignment operator or null if simply assignment
     * @param data     the data
     * @return the left hand side
     */
    protected Object executeAssign(final JexlNode node, final JexlOperator assignop, final Object data) { // CSOFF: MethodLength
        cancelCheck(node);
        // left contains the reference to assign to
        final JexlNode left = node.jjtGetChild(0);
        ASTIdentifier var = null;
        Object object = null;
        int symbol = -1;
        // check var decl with assign is ok
        if (left instanceof ASTIdentifier) {
            var = (ASTIdentifier) left;
            symbol = var.getSymbol();
            if (symbol >= 0 && options.isLexical()) {
                if (var instanceof ASTVar) {
                    if (!defineVariable((ASTVar) var, block)) {
                        return redefinedVariable(var, var.getName());
                    }
                } else if (options.isLexicalShade() && var.isShaded()) { 
                    return undefinedVariable(var, var.getName());
                }
            }
        }
        boolean antish = options.isAntish();
        // 0: determine initial object & property:
        final int last = left.jjtGetNumChildren() - 1;
        // right is the value expression to assign
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        // a (var?) v = ... expression
        if (var != null) {
            if (symbol >= 0) {
                // check we are not assigning a symbol itself
                if (last < 0) {
                    if (assignop != null) {
                        final Object self = getVariable(frame, block, var);
                        right = operators.tryAssignOverload(node, assignop, self, right);
                        if (right == JexlOperator.ASSIGN) {
                            return self;
                        }
                    }
                    frame.set(symbol, right);
                    // make the closure accessible to itself, ie capture the currently set variable after frame creation
                    if (right instanceof Closure) {
                        ((Closure) right).setCaptured(symbol, right);
                    }
                    return right; // 1
                }
                object = getVariable(frame, block, var);
                // top level is a symbol, can not be an antish var
                antish = false;
            } else {
                // check we are not assigning direct global
                if (last < 0) {
                    if (assignop != null) {
                        final Object self = context.get(var.getName());
                        right = operators.tryAssignOverload(node, assignop, self, right);
                        if (right == JexlOperator.ASSIGN) {
                            return self;
                        }
                    }
                    setContextVariable(node, var.getName(), right);
                    return right; // 2
                }
                object = context.get(var.getName());
                // top level accesses object, can not be an antish var
                if (object != null) {
                    antish = false;
                }
            }
        } else if (!(left instanceof ASTReference)) {
            throw new JexlException(left, "illegal assignment form 0");
        }
        // 1: follow children till penultimate, resolve dot/array
        JexlNode objectNode = null;
        StringBuilder ant = null;
        int v = 1;
        // start at 1 if symbol
        main: for (int c = symbol >= 0 ? 1 : 0; c < last; ++c) {
            objectNode = left.jjtGetChild(c);
            object = objectNode.jjtAccept(this, object);
            if (object != null) {
                // disallow mixing antish variable & bean with same root; avoid ambiguity
                antish = false;
            } else if (antish) {
                // initialize if first time
                if (ant == null) {
                    final JexlNode first = left.jjtGetChild(0);
                    final ASTIdentifier firstId = first instanceof ASTIdentifier
                            ? (ASTIdentifier) first
                            : null;
                    if (firstId != null && firstId.getSymbol() < 0) {
                        ant = new StringBuilder(firstId.getName());
                    } else {
                        // ant remains null, object is null, stop solving
                        antish = false;
                        break main;
                    }
                }
                // catch up to current child
                for (; v <= c; ++v) {
                    final JexlNode child = left.jjtGetChild(v);
                    final ASTIdentifierAccess aid = child instanceof ASTIdentifierAccess
                            ? (ASTIdentifierAccess) child
                            : null;
                    // remain antish only if unsafe navigation
                    if (aid != null && !aid.isSafe() && !aid.isExpression()) {
                        ant.append('.');
                        ant.append(aid.getName());
                    } else {
                        antish = false;
                        break main;
                    }
                }
                // solve antish
                object = context.get(ant.toString());
            } else {
                throw new JexlException(objectNode, "illegal assignment form");
            }
        }
        // 2: last objectNode will perform assignement in all cases
        Object property = null;
        JexlNode propertyNode = left.jjtGetChild(last);
        final ASTIdentifierAccess propertyId = propertyNode instanceof ASTIdentifierAccess
                ? (ASTIdentifierAccess) propertyNode
                : null;
        if (propertyId != null) {
            // deal with creating/assignining antish variable
            if (antish && ant != null && object == null && !propertyId.isSafe() && !propertyId.isExpression()) {
                if (last > 0) {
                    ant.append('.');
                }
                ant.append(propertyId.getName());
                if (assignop != null) {
                    final Object self = context.get(ant.toString());
                    right = operators.tryAssignOverload(node, assignop, self, right);
                    if (right == JexlOperator.ASSIGN) {
                        return self;
                    }
                }
                setContextVariable(propertyNode, ant.toString(), right);
                return right; // 3
            }
            // property of an object ?
            property = evalIdentifier(propertyId);
        } else if (propertyNode instanceof ASTArrayAccess) {
            // can have multiple nodes - either an expression, integer literal or reference
            final int numChildren = propertyNode.jjtGetNumChildren() - 1;
            for (int i = 0; i < numChildren; i++) {
                final JexlNode nindex = propertyNode.jjtGetChild(i);
                final Object index = nindex.jjtAccept(this, null);
                object = getAttribute(object, index, nindex);
            }
            propertyNode = propertyNode.jjtGetChild(numChildren);
            property = propertyNode.jjtAccept(this, null);
        } else {
            throw new JexlException(objectNode, "illegal assignment form");
        }
        // we may have a null property as in map[null], no check needed.
        // we can not *have* a null object though.
        if (object == null) {
            // no object, we fail
            return unsolvableProperty(objectNode, "<null>.<?>", true, null);
        }
        // 3: one before last, assign
        if (assignop != null) {
            final Object self = getAttribute(object, property, propertyNode);
            right = operators.tryAssignOverload(node, assignop, self, right);
            if (right == JexlOperator.ASSIGN) {
                return self;
            }
        }
        setAttribute(object, property, right, propertyNode);
        return right; // 4
    }

    @Override
    protected Object[] visit(final ASTArguments node, final Object data) {
        final int argc = node.jjtGetNumChildren();
        final Object[] argv = new Object[argc];
        for (int i = 0; i < argc; i++) {
            argv[i] = node.jjtGetChild(i).jjtAccept(this, data);
        }
        return argv;
    }

    @Override
    protected Object visit(final ASTMethodNode node, final Object data) {
        return visit(node, null, data);
    }

    /**
     * Execute a method call, ie syntactically written as name.call(...).
     * @param node the actual method call node
     * @param object non null when name.call is an antish variable
     * @param data the context
     * @return the method call result
     */
    private Object visit(final ASTMethodNode node, Object object, final Object data) {
        // left contains the reference to the method
        final JexlNode methodNode = node.jjtGetChild(0);
        Object method;
        // 1: determine object and method or functor
        if (methodNode instanceof ASTIdentifierAccess) {
            method = methodNode;
            if (object == null) {
                object = data;
                if (object == null) {
                    // no object, we fail
                    return node.isSafeLhs(isSafe())
                        ? null
                        : unsolvableMethod(methodNode, "<null>.<?>(...)");
                }
            } else {
                // edge case of antish var used as functor
                method = object;
            }
        } else {
            method = methodNode.jjtAccept(this, data);
        }
        Object result = method;
        for (int a = 1; a < node.jjtGetNumChildren(); ++a) {
            if (result == null) {
                // no method, we fail// variable unknown in context and not a local
                return node.isSafeLhs(isSafe())
                        ? null
                        : unsolvableMethod(methodNode, "<?>.<null>(...)");
            }
            final ASTArguments argNode = (ASTArguments) node.jjtGetChild(a);
            result = call(node, object, result, argNode);
            object = result;
        }
        return result;
    }

    @Override
    protected Object visit(final ASTFunctionNode node, final Object data) {
        final ASTIdentifier functionNode = (ASTIdentifier) node.jjtGetChild(0);
        final String nsid = functionNode.getNamespace();
        final Object namespace = (nsid != null)? resolveNamespace(nsid, node) : context;
        final ASTArguments argNode = (ASTArguments) node.jjtGetChild(1);
        return call(node, namespace, functionNode, argNode);
    }

    /**
     * Calls a method (or function).
     * <p>
     * Method resolution is a follows:
     * 1 - attempt to find a method in the target passed as parameter;
     * 2 - if this fails, seeks a JexlScript or JexlMethod or a duck-callable* as a property of that target;
     * 3 - if this fails, narrow the arguments and try again 1
     * 4 - if this fails, seeks a context or arithmetic method with the proper name taking the target as first argument;
     * </p>
     * *duck-callable: an object where a "call" function exists
     *
     * @param node    the method node
     * @param target  the target of the method, what it should be invoked upon
     * @param functor the object carrying the method or function or the method identifier
     * @param argNode the node carrying the arguments
     * @return the result of the method invocation
     */
    protected Object call(final JexlNode node, final Object target, Object functor, final ASTArguments argNode) {
        cancelCheck(node);
        // evaluate the arguments
        final Object[] argv = visit(argNode, null);
        // get the method name if identifier
        final int symbol;
        final String methodName;
        boolean cacheable = cache;
        boolean isavar = false;
        if (functor instanceof ASTIdentifier) {
            // function call, target is context or namespace (if there was one)
            final ASTIdentifier methodIdentifier = (ASTIdentifier) functor;
            symbol = methodIdentifier.getSymbol();
            methodName = methodIdentifier.getName();
            functor = null;
            // is it a global or local variable ?
            if (target == context) {
                if (frame != null && frame.has(symbol)) {
                    functor = frame.get(symbol);
                    isavar = functor != null;
                } else if (context.has(methodName)) {
                    functor = context.get(methodName);
                    isavar = functor != null;
                }
                // name is a variable, cant be cached
                cacheable &= !isavar;
            }
        } else if (functor instanceof ASTIdentifierAccess) {
            // a method call on target
            methodName = ((ASTIdentifierAccess) functor).getName();
            symbol = -1;
            functor = null;
            cacheable = true;
        } else if (functor != null) {
            // ...(x)(y)
            symbol = -1 - 1; // -2;
            methodName = null;
            cacheable = false;
        } else if (!node.isSafeLhs(isSafe())) {
            return unsolvableMethod(node, "?(...)");
        } else {
            // safe lhs
            return null;
        }

        // solving the call site
        final CallDispatcher call = new CallDispatcher(node, cacheable);
        try {
            // do we have a  cached version method/function name ?
            final Object eval = call.tryEval(target, methodName, argv);
            if (JexlEngine.TRY_FAILED != eval) {
                return eval;
            }
            boolean functorp = false;
            boolean narrow = false;
            // pseudo loop to try acquiring methods without and with argument narrowing
            while (true) {
                call.narrow = narrow;
                // direct function or method call
                if (functor == null || functorp) {
                    // try a method or function from context
                    if (call.isTargetMethod(target, methodName, argv)) {
                        return call.eval(methodName);
                    }
                    if (target == context) {
                        // solve 'null' namespace
                        final Object namespace = resolveNamespace(null, node);
                        if (namespace != null
                            && namespace != context
                            && call.isTargetMethod(namespace, methodName, argv)) {
                            return call.eval(methodName);
                        }
                        // do not try context function since this was attempted
                        // 10 lines above...; solve as an arithmetic function
                        if (call.isArithmeticMethod(methodName, argv)) {
                            return call.eval(methodName);
                        }
                        // could not find a method, try as a property of a non-context target (performed once)
                    } else {
                        // try prepending target to arguments and look for
                        // applicable method in context...
                        final Object[] pargv = functionArguments(target, narrow, argv);
                        if (call.isContextMethod(methodName, pargv)) {
                            return call.eval(methodName);
                        }
                        // ...or arithmetic
                        if (call.isArithmeticMethod(methodName, pargv)) {
                            return call.eval(methodName);
                        }
                        // the method may also be a functor stored in a property of the target
                        if (!narrow) {
                            final JexlPropertyGet get = uberspect.getPropertyGet(target, methodName);
                            if (get != null) {
                                functor = get.tryInvoke(target, methodName);
                                functorp = functor != null;
                            }
                        }
                    }
                }
                // this may happen without the above when we are chaining call like x(a)(b)
                // or when a var/symbol or antish var is used as a "function" name
                if (functor != null) {
                    // lambda, script or jexl method will do
                    if (functor instanceof JexlScript) {
                        return ((JexlScript) functor).execute(context, argv);
                    }
                    if (functor instanceof JexlMethod) {
                        return ((JexlMethod) functor).invoke(target, argv);
                    }
                    final String mCALL = "call";
                    // may be a generic callable, try a 'call' method
                    if (call.isTargetMethod(functor, mCALL, argv)) {
                        return call.eval(mCALL);
                    }
                    // functor is a var, may be method is a global one ?
                    if (isavar && target == context) {
                        if (call.isContextMethod(methodName, argv)) {
                            return call.eval(methodName);
                        }
                        if (call.isArithmeticMethod(methodName, argv)) {
                            return call.eval(methodName);
                        }
                    }
                    // try prepending functor to arguments and look for
                    // context or arithmetic function called 'call'
                    final Object[] pargv = functionArguments(functor, narrow, argv);
                    if (call.isContextMethod(mCALL, pargv)) {
                        return call.eval(mCALL);
                    }
                    if (call.isArithmeticMethod(mCALL, pargv)) {
                        return call.eval(mCALL);
                    }
                }
                // if we did not find an exact method by name and we haven't tried yet,
                // attempt to narrow the parameters and if this succeeds, try again in next loop
                if (!narrow && arithmetic.narrowArguments(argv)) {
                    narrow = true;
                    // continue;
                } else {
                    break;
                }
            }
            // we have either evaluated and returned or no method was found
            return node.isSafeLhs(isSafe())
                    ? null
                    : unsolvableMethod(node, methodName, argv);
        } catch (final JexlException.TryFailed xany) {
            throw invocationException(node, methodName, xany);
        } catch (final JexlException xthru) {
            throw xthru;
        } catch (final Exception xany) {
            throw invocationException(node, methodName, xany);
        }
    }

    @Override
    protected Object visit(final ASTConstructorNode node, final Object data) {
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }
        // first child is class or class name
        final Object target = node.jjtGetChild(0).jjtAccept(this, data);
        // get the ctor args
        final int argc = node.jjtGetNumChildren() - 1;
        Object[] argv = new Object[argc];
        for (int i = 0; i < argc; i++) {
            argv[i] = node.jjtGetChild(i + 1).jjtAccept(this, data);
        }

        try {
            final boolean cacheable = cache;
            // attempt to reuse last funcall cached in volatile JexlNode.value
            if (cacheable) {
                final Object cached = node.jjtGetValue();
                if (cached instanceof Funcall) {
                    final Object eval = ((Funcall) cached).tryInvoke(this, null, target, argv);
                    if (JexlEngine.TRY_FAILED != eval) {
                        return eval;
                    }
                }
            }
            boolean narrow = false;
            JexlMethod ctor = null;
            Funcall funcall = null;
            while (true) {
                // try as stated
                ctor = uberspect.getConstructor(target, argv);
                if (ctor != null) {
                    if (cacheable && ctor.isCacheable()) {
                        funcall = new Funcall(ctor, narrow);
                    }
                    break;
                }
                // try with prepending context as first argument
                final Object[] nargv = callArguments(context, narrow, argv);
                ctor = uberspect.getConstructor(target, nargv);
                if (ctor != null) {
                    if (cacheable && ctor.isCacheable()) {
                        funcall = new ContextualCtor(ctor, narrow);
                    }
                    argv = nargv;
                    break;
                }
                // if we did not find an exact method by name and we haven't tried yet,
                // attempt to narrow the parameters and if this succeeds, try again in next loop
                if (!narrow && arithmetic.narrowArguments(argv)) {
                    narrow = true;
                    continue;
                }
                // we are done trying
                break;
            }
            // we have either evaluated and returned or might have found a ctor
            if (ctor != null) {
                final Object eval = ctor.invoke(target, argv);
                // cache executor in volatile JexlNode.value
                if (funcall != null) {
                    node.jjtSetValue(funcall);
                }
                return eval;
            }
            final String tstr = target != null ? target.toString() : "?";
            return unsolvableMethod(node, tstr, argv);
        } catch (final JexlException.Method xmethod) {
            throw xmethod;
        } catch (final Exception xany) {
            final String tstr = target != null ? target.toString() : "?";
            throw invocationException(node, tstr, xany);
        }
    }

    @Override
    protected Object visit(final ASTJxltLiteral node, final Object data) {
        TemplateEngine.TemplateExpression tp = (TemplateEngine.TemplateExpression) node.jjtGetValue();
        if (tp == null) {
            final TemplateEngine jxlt = jexl.jxlt();
            JexlInfo info = node.jexlInfo();
            if (this.block != null) {
                info = new JexlNode.Info(node, info);
            }
            tp = jxlt.parseExpression(info, node.getLiteral(), frame != null ? frame.getScope() : null);
            node.jjtSetValue(tp);
        }
        if (tp != null) {
            return tp.evaluate(frame, context);
        }
        return null;
    }

    @Override
    protected Object visit(final ASTAnnotation node, final Object data) {
        throw new UnsupportedOperationException(ASTAnnotation.class.getName() + ": Not supported.");
    }

    @Override
    protected Object visit(final ASTAnnotatedStatement node, final Object data) {
        return processAnnotation(node, 0, data);
    }

    /**
     * An annotated call.
     */
    public class AnnotatedCall implements Callable<Object> {
        /** The statement. */
        private final ASTAnnotatedStatement stmt;
        /** The child index. */
        private final int index;
        /** The data. */
        private final Object data;
        /** Tracking whether we processed the annotation. */
        private boolean processed = false;

        /**
         * Simple ctor.
         * @param astmt the statement
         * @param aindex the index
         * @param adata the data
         */
        AnnotatedCall(final ASTAnnotatedStatement astmt, final int aindex, final Object adata) {
            stmt = astmt;
            index = aindex;
            data = adata;
        }


        @Override
        public Object call() throws Exception {
            processed = true;
            try {
                return processAnnotation(stmt, index, data);
            } catch (final JexlException.Return | JexlException.Break | JexlException.Continue xreturn) {
                return xreturn;
            }
        }
        
        /**
         * @return whether the statement has been processed
         */
        public boolean isProcessed() {
            return processed;
        }
        
        /**
         * @return the actual statement.
         */
        public Object getStatement() {
            return stmt;
        }
    }
    
    /**
     * Processes an annotated statement.
     * @param stmt the statement
     * @param index the index of the current annotation being processed
     * @param data the contextual data
     * @return  the result of the statement block evaluation
     */
    protected Object processAnnotation(final ASTAnnotatedStatement stmt, final int index, final Object data) {
        // are we evaluating the block ?
        final int last = stmt.jjtGetNumChildren() - 1;
        if (index == last) {
            final JexlNode cblock = stmt.jjtGetChild(last);
            // if the context has changed, might need a new interpreter
            final JexlArithmetic jexla = arithmetic.options(context);
            if (jexla != arithmetic) {
                if (!arithmetic.getClass().equals(jexla.getClass())) {
                    logger.warn("expected arithmetic to be " + arithmetic.getClass().getSimpleName()
                            + ", got " + jexla.getClass().getSimpleName()
                    );
                }
                final Interpreter ii = new Interpreter(Interpreter.this, jexla);
                final Object r = cblock.jjtAccept(ii, data);
                if (ii.isCancelled()) {
                    Interpreter.this.cancel();
                }
                return r;
            } else {
                return cblock.jjtAccept(Interpreter.this, data);
            }
        }
        // tracking whether we processed the annotation
        final AnnotatedCall jstmt = new AnnotatedCall(stmt, index + 1, data);
        // the annotation node and name
        final ASTAnnotation anode = (ASTAnnotation) stmt.jjtGetChild(index);
        final String aname = anode.getName();
        // evaluate the arguments
        final Object[] argv = anode.jjtGetNumChildren() > 0
                        ? visit((ASTArguments) anode.jjtGetChild(0), null) : null;
        // wrap the future, will recurse through annotation processor
        final Object result;
        try {
            result = processAnnotation(aname, argv, jstmt);
            // not processing an annotation is an error
            if (!jstmt.isProcessed()) {
                return annotationError(anode, aname, null);
            }
        } catch (final JexlException xany) {
            throw xany;
        } catch (final Exception xany) {
            return annotationError(anode, aname, xany);
        }
        // the caller may return a return, break or continue
        if (result instanceof JexlException) {
            throw (JexlException) result;
        }
        return result;
    }

    /**
     * Delegates the annotation processing to the JexlContext if it is an AnnotationProcessor.
     * @param annotation    the annotation name
     * @param args          the annotation arguments
     * @param stmt          the statement / block that was annotated
     * @return the result of statement.call()
     * @throws Exception if anything goes wrong
     */
    protected Object processAnnotation(final String annotation, final Object[] args, final Callable<Object> stmt) throws Exception {
                return context instanceof JexlContext.AnnotationProcessor
                ? ((JexlContext.AnnotationProcessor) context).processAnnotation(annotation, args, stmt)
                : stmt.call();
    }
}
