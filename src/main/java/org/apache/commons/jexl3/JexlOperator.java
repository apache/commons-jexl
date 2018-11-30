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

/**
 * The JEXL operators.
 *
 * These are the operators that are executed by JexlArithmetic methods.
 *
 * <p>Each of them  associates a symbol to a method signature.
 * For instance, '+' is associated to 'T add(L x, R y)'.</p>
 *
 * <p>The default JexlArithmetic implements generic versions of these methods using Object as arguments.
 * You can use your own derived JexlArithmetic that override and/or overload those operator methods.
 * Note that these are overloads by convention, not actual Java overloads.
 * The following rules apply to operator methods:</p>
 * <ul>
 * <li>Operator methods should be public</li>
 * <li>Operators return type should be respected when primitive (int, boolean,...)</li>
 * <li>Operators may be overloaded multiple times with different signatures</li>
 * <li>Operators may return JexlEngine.TRY_AGAIN to fallback on default JEXL implementation</li>
 * </ul>
 *
 * @since 3.0
 */
public enum JexlOperator {

    /**
     * Add operator.
     * <br><strong>Syntax:</strong> <code>x + y</code>
     * <br><strong>Method:</strong> <code>T add(L x, R y);</code>.
     * @see JexlArithmetic#add
     */
    ADD("+", "add", 2),

    /**
     * Subtract operator.
     * <br><strong>Syntax:</strong> <code>x - y</code>
     * <br><strong>Method:</strong> <code>T subtract(L x, R y);</code>.
     * @see JexlArithmetic#subtract
     */
    SUBTRACT("-", "subtract", 2),

    /**
     * Multiply operator.
     * <br><strong>Syntax:</strong> <code>x * y</code>
     * <br><strong>Method:</strong> <code>T multiply(L x, R y);</code>.
     * @see JexlArithmetic#multiply
     */
    MULTIPLY("*", "multiply", 2),

    /**
     * Divide operator.
     * <br><strong>Syntax:</strong> <code>x / y</code>
     * <br><strong>Method:</strong> <code>T divide(L x, R y);</code>.
     * @see JexlArithmetic#divide
     */
    DIVIDE("/", "divide", 2),

    /**
     * Modulo operator.
     * <br><strong>Syntax:</strong> <code>x % y</code>
     * <br><strong>Method:</strong> <code>T mod(L x, R y);</code>.
     * @see JexlArithmetic#mod
     */
    MOD("%", "mod", 2),

    /**
     * Bitwise-and operator.
     * <br><strong>Syntax:</strong> <code>x &amp; y</code>
     * <br><strong>Method:</strong> <code>T and(L x, R y);</code>.
     * @see JexlArithmetic#and
     */
    AND("&", "and", 2),

    /**
     * Bitwise-or operator.
     * <br><strong>Syntax:</strong> <code>x | y</code>
     * <br><strong>Method:</strong> <code>T or(L x, R y);</code>.
     * @see JexlArithmetic#or
     */
    OR("|", "or", 2),

    /**
     * Bitwise-xor operator.
     * <br><strong>Syntax:</strong> <code>x ^ y</code>
     * <br><strong>Method:</strong> <code>T xor(L x, R y);</code>.
     * @see JexlArithmetic#xor
     */
    XOR("^", "xor", 2),

    /**
     * Left-shift operator.
     * <br><strong>Syntax:</strong> <code>x << y</code>
     * <br><strong>Method:</strong> <code>T leftShift(L x, R y);</code>.
     * @see JexlArithmetic#leftShift
     */
    SHL("<<", "leftShift", 2),

    /**
     * Right-shift operator.
     * <br><strong>Syntax:</strong> <code>x >> y</code>
     * <br><strong>Method:</strong> <code>T rightShift(L x, R y);</code>.
     * @see JexlArithmetic#rightShift
     */
    SAR(">>", "rightShift", 2),

    /**
     * Right-shift unsigned operator.
     * <br><strong>Syntax:</strong> <code>x >>> y</code>
     * <br><strong>Method:</strong> <code>T rightShiftUnsigned(L x, R y);</code>.
     * @see JexlArithmetic#rightShiftUnsigned
     */
    SHR(">>>", "rightShiftUnsigned", 2),

    /**
     * Equals operator.
     * <br><strong>Syntax:</strong> <code>x == y</code>
     * <br><strong>Method:</strong> <code>boolean equals(L x, R y);</code>.
     * @see JexlArithmetic#equals
     */
    EQ("==", "equals", 2),

    /**
     * Less-than operator.
     * <br><strong>Syntax:</strong> <code>x &lt; y</code>
     * <br><strong>Method:</strong> <code>boolean lessThan(L x, R y);</code>.
     * @see JexlArithmetic#lessThan
     */
    LT("<", "lessThan", 2),

    /**
     * Less-than-or-equal operator.
     * <br><strong>Syntax:</strong> <code>x &lt;= y</code>
     * <br><strong>Method:</strong> <code>boolean lessThanOrEqual(L x, R y);</code>.
     * @see JexlArithmetic#lessThanOrEqual
     */
    LTE("<=", "lessThanOrEqual", 2),

    /**
     * Greater-than operator.
     * <br><strong>Syntax:</strong> <code>x &gt; y</code>
     * <br><strong>Method:</strong> <code>boolean greaterThan(L x, R y);</code>.
     * @see JexlArithmetic#greaterThan
     */
    GT(">", "greaterThan", 2),

    /**
     * Greater-than-or-equal operator.
     * <br><strong>Syntax:</strong> <code>x &gt;= y</code>
     * <br><strong>Method:</strong> <code>boolean greaterThanOrEqual(L x, R y);</code>.
     * @see JexlArithmetic#greaterThanOrEqual
     */
    GTE(">=", "greaterThanOrEqual", 2),

    /**
     * Contains operator.
     * <br><strong>Syntax:</strong> <code>x =~ y</code>
     * <br><strong>Method:</strong> <code>boolean contains(L x, R y);</code>.
     * @see JexlArithmetic#contains
     */
    CONTAINS("=~", "contains", 2),

    /**
     * Starts-with operator.
     * <br><strong>Syntax:</strong> <code>x =^ y</code>
     * <br><strong>Method:</strong> <code>boolean startsWith(L x, R y);</code>.
     * @see JexlArithmetic#startsWith
     */
    STARTSWITH("=^", "startsWith", 2),

    /**
     * Ends-with operator.
     * <br><strong>Syntax:</strong> <code>x =$ y</code>
     * <br><strong>Method:</strong> <code>boolean endsWith(L x, R y);</code>.
     * @see JexlArithmetic#endsWith
     */
    ENDSWITH("=$", "endsWith", 2),

    /**
     * Not operator.
     * <br><strong>Syntax:</strong> <code>!x</code>
     * <br><strong>Method:</strong> <code>T not(L x);</code>.
     * @see JexlArithmetic#not
     */
    NOT("!", "not", 1),

    /**
     * Complement operator.
     * <br><strong>Syntax:</strong> <code>~x</code>
     * <br><strong>Method:</strong> <code>T complement(L x);</code>.
     * @see JexlArithmetic#complement
     */
    COMPLEMENT("~", "complement", 1),

    /**
     * Negate operator.
     * <br><strong>Syntax:</strong> <code>-x</code>
     * <br><strong>Method:</strong> <code>T negate(L x);</code>.
     * @see JexlArithmetic#negate
     */
    NEGATE("-", "negate", 1),

    /**
     * Confirm operator.
     * <br><strong>Syntax:</strong> <code>+x</code>
     * <br><strong>Method:</strong> <code>T confirm(L x);</code>.
     * @see JexlArithmetic#confirm
     */
    CONFIRM("+", "confirm", 1),

    /**
     * Empty operator.
     * <br><strong>Syntax:</strong> <code>empty x</code> or <code>empty(x)</code>
     * <br><strong>Method:</strong> <code>boolean isEmpty(L x);</code>.
     * @see JexlArithmetic#isEmpty
     */
    EMPTY("empty", "empty", 1),

    /**
     * Size operator.
     * <br><strong>Syntax:</strong> <code>size x</code> or <code>size(x)</code>
     * <br><strong>Method:</strong> <code>int size(L x);</code>.
     * @see JexlArithmetic#size
     */
    SIZE("size", "size", 1),

    /**
     * Self-add operator.
     * <br><strong>Syntax:</strong> <code>x += y</code>
     * <br><strong>Method:</strong> <code>T selfAdd(L x, R y);</code>.
     */
    SELF_ADD("+=", "selfAdd", ADD),

    /**
     * Self-subtract operator.
     * <br><strong>Syntax:</strong> <code>x -= y</code>
     * <br><strong>Method:</strong> <code>T selfSubtract(L x, R y);</code>.
     */
    SELF_SUBTRACT("-=", "selfSubtract", SUBTRACT),

    /**
     * Self-multiply operator.
     * <br><strong>Syntax:</strong> <code>x *= y</code>
     * <br><strong>Method:</strong> <code>T selfMultiply(L x, R y);</code>.
     */
    SELF_MULTIPLY("*=", "selfMultiply", MULTIPLY),

    /**
     * Self-divide operator.
     * <br><strong>Syntax:</strong> <code>x /= y</code>
     * <br><strong>Method:</strong> <code>T selfDivide(L x, R y);</code>.
     */
    SELF_DIVIDE("/=", "selfDivide", DIVIDE),

    /**
     * Self-modulo operator.
     * <br><strong>Syntax:</strong> <code>x %= y</code>
     * <br><strong>Method:</strong> <code>T selfMod(L x, R y);</code>.
     */
    SELF_MOD("%=", "selfMod", MOD),

    /**
     * Self-and operator.
     * <br><strong>Syntax:</strong> <code>x &amp;= y</code>
     * <br><strong>Method:</strong> <code>T selfAnd(L x, R y);</code>.
     */
    SELF_AND("&=", "selfAnd", AND),

    /**
     * Self-or operator.
     * <br><strong>Syntax:</strong> <code>x |= y</code>
     * <br><strong>Method:</strong> <code>T selfOr(L x, R y);</code>.
     */
    SELF_OR("|=", "selfOr", OR),

    /**
     * Self-xor operator.
     * <br><strong>Syntax:</strong> <code>x ^= y</code>
     * <br><strong>Method:</strong> <code>T selfXor(L x, R y);</code>.
     */
    SELF_XOR("^=", "selfXor", XOR),

    /**
     * Self-shl operator.
     * <br><strong>Syntax:</strong> <code>x <<= y</code>
     * <br><strong>Method:</strong> <code>T selfLeftShift(L x, R y);</code>.
     */
    SELF_SHL("<<=", "selfLeftShift", SHL),

    /**
     * Self-sar operator.
     * <br><strong>Syntax:</strong> <code>x >>= y</code>
     * <br><strong>Method:</strong> <code>T selfRightShift(L x, R y);</code>.
     */
    SELF_SAR(">>=", "selfRightShift", SAR),

    /**
     * Self-shr operator.
     * <br><strong>Syntax:</strong> <code>x >>>= y</code>
     * <br><strong>Method:</strong> <code>T selfRightShiftUnsigned(L x, R y);</code>.
     */
    SELF_SHR(">>>=", "selfRightShiftUnsigned", SHR),

    /**
     * Increment operator.
     * <br><strong>Syntax:</strong> <code>++x</code>
     * <br><strong>Method:</strong> <code>T increment(L x);</code>.
     */
    INCREMENT("++", "increment", 1),

    /**
     * Increment operator.
     * <br><strong>Syntax:</strong> <code>--x</code>
     * <br><strong>Method:</strong> <code>T decrement(L x);</code>.
     */
    DECREMENT("--", "decrement", 1),

    /**
     * Indirect operator.
     * <br><strong>Syntax:</strong> <code>*x</code>
     * <br><strong>Method:</strong> <code>T indirect(L x);</code>.
     */
    INDIRECT("*", "indirect", 1),

    /**
     * Indirect assign operator.
     * <br><strong>Syntax:</strong> <code>*x = y</code>
     * <br><strong>Method:</strong> <code>T indirectAssign(L x, R y);</code>.
     */
    INDIRECT_ASSIGN("*=", "indirectAssign", 2),

    /**
     * Marker for side effect.
     * <br>Returns this from 'self*' overload method to let the engine know the side effect has been performed and
     * there is no need to assign the result.
     */
    ASSIGN("=", null, null),

    /**
     * Property get operator as in: x.y.
     * <br><strong>Syntax:</strong> <code>x.y</code>
     * <br><strong>Method:</strong> <code>Object propertyGet(L x, R y);</code>.
     */
    PROPERTY_GET(".", "propertyGet", 2),

    /**
     * Property set operator as in: x.y = z.
     * <br><strong>Syntax:</strong> <code>x.y = z</code>
     * <br><strong>Method:</strong> <code>void propertySet(L x, R y, V z);</code>.
     */
    PROPERTY_SET(".=", "propertySet", 3),

    /**
     * Array get operator as in: x[y].
     * <br><strong>Syntax:</strong> <code>x.y</code>
     * <br><strong>Method:</strong> <code>Object arrayGet(L x, R y);</code>.
     */
    ARRAY_GET("[]", "arrayGet", 2),

    /**
     * Array set operator as in: x[y] = z.
     * <br><strong>Syntax:</strong> <code>x[y] = z</code>
     * <br><strong>Method:</strong> <code>void arraySet(L x, R y, V z);</code>.
     */
    ARRAY_SET("[]=", "arraySet", 3),

    /**
     * Iterator generator as in for(var x : y).
     * If the returned Iterator is AutoCloseable, close will be called after the last execution of the loop block.
     * <br><strong>Syntax:</strong> <code>for(var x : y){...} </code>
     * <br><strong>Method:</strong> <code>Iterator&lt;Object&gt; forEach(R y);</code>.
     * @since 3.1
     */
    FOR_EACH("for(x:y)", "forEach", 1),

    /**
     * Iterator generator as in for(var i,j : y).
     * If the returned Iterator is AutoCloseable, close will be called after the last execution of the loop block.
     * <br><strong>Syntax:</strong> <code>for(var i,j : y){...} </code>
     * <br><strong>Method:</strong> <code>Iterator&lt;Object&gt; forEachIndexed(R y);</code>.
     * @since 3.2
     */
    FOR_EACH_INDEXED("for(i,j:y)", "forEachIndexed", 1),

    /**
     * Resource generator as in try(var x : y).
     * If the returned Resource is AutoCloseable, close will be called after the execution of the statement.
     * <br><strong>Syntax:</strong> <code>try(var x : y){...} </code>
     * <br><strong>Method:</strong> <code>Object tryWith(R y);</code>.
     * @since 3.2
     */
    TRY_WITH("try(x:y)", "tryWith", 1);

    /**
     * The operator symbol.
     */
    private final String operator;

    /**
     * The associated operator method name.
     */
    private final String methodName;

    /**
     * The method arity (ie number of arguments).
     */
    private final int arity;

    /**
     * The base operator.
     */
    private final JexlOperator base;

    /**
     * Creates a base operator.
     *
     * @param o    the operator name
     * @param m    the method name associated to this operator in a JexlArithmetic
     * @param argc the number of parameters for the method
     */
    JexlOperator(String o, String m, int argc) {
        this.operator = o;
        this.methodName = m;
        this.arity = argc;
        this.base = null;
    }

    /**
     * Creates a side-effect operator.
     *
     * @param o the operator name
     * @param m the method name associated to this operator in a JexlArithmetic
     * @param b the base operator, ie + for +=
     */
    JexlOperator(String o, String m, JexlOperator b) {
        this.operator = o;
        this.methodName = m;
        this.arity = 2;
        this.base = b;
    }

    /**
     * Gets this operator symbol.
     *
     * @return the symbol
     */
    public final String getOperatorSymbol() {
        return operator;
    }

    /**
     * Gets this operator method name in a JexlArithmetic.
     *
     * @return the method name
     */
    public final String getMethodName() {
        return methodName;
    }

    /**
     * Gets this operator number of parameters.
     *
     * @return the method arity
     */
    public int getArity() {
        return arity;
    }

    /**
     * Gets the base operator.
     *
     * @return the base operator
     */
    public final JexlOperator getBaseOperator() {
        return base;
    }

}
