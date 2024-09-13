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
 * The following rules apply to all operator methods:</p>
 * <ul>
 * <li>Operator methods should be public</li>
 * <li>Operators return type should be respected when primitive (int, boolean,...)</li>
 * <li>Operators may be overloaded multiple times with different signatures</li>
 * <li>Operators may return JexlEngine.TRY_AGAIN to fallback on default JEXL implementation</li>
 * </ul>
 *
 * For side effect operators, operators that modify the left-hand size value (+=, -=, etc), the user implemented
 * overload methods may return:
 * <ul>
 *     <li>JexlEngine.TRY_FAIL to let the default fallback behavior be executed.</li>
 *     <li>Any other value will be used as the new value to be assigned to the left-hand-side.</li>
 * </ul>
 * Note that side effect operators always return the left-hand side value (with an exception for postfix ++ and --).
 *
 * @since 3.0
 */
public enum JexlOperator {
    /**
     * Add operator.
     * <br><strong>Syntax:</strong> {@code x + y}
     * <br><strong>Method:</strong> {@code T add(L x, R y);}.
     * @see JexlArithmetic#add(Object, Object)
     */
    ADD("+", "add", 2),

    /**
     * Subtract operator.
     * <br><strong>Syntax:</strong> {@code x - y}
     * <br><strong>Method:</strong> {@code T subtract(L x, R y);}.
     * @see JexlArithmetic#subtract(Object, Object)
     */
    SUBTRACT("-", "subtract", 2),

    /**
     * Multiply operator.
     * <br><strong>Syntax:</strong> {@code x * y}
     * <br><strong>Method:</strong> {@code T multiply(L x, R y);}.
     * @see JexlArithmetic#multiply(Object, Object)
     */
    MULTIPLY("*", "multiply", 2),

    /**
     * Divide operator.
     * <br><strong>Syntax:</strong> {@code x / y}
     * <br><strong>Method:</strong> {@code T divide(L x, R y);}.
     * @see JexlArithmetic#divide(Object, Object)
     */
    DIVIDE("/", "divide", 2),

    /**
     * Modulo operator.
     * <br><strong>Syntax:</strong> {@code x % y}
     * <br><strong>Method:</strong> {@code T mod(L x, R y);}.
     * @see JexlArithmetic#mod(Object, Object)
     */
    MOD("%", "mod", 2),

    /**
     * Bitwise-and operator.
     * <br><strong>Syntax:</strong> {@code x & y}
     * <br><strong>Method:</strong> {@code T and(L x, R y);}.
     * @see JexlArithmetic#and(Object, Object)
     */
    AND("&", "and", 2),

    /**
     * Bitwise-or operator.
     * <br><strong>Syntax:</strong> {@code x | y}
     * <br><strong>Method:</strong> {@code T or(L x, R y);}.
     * @see JexlArithmetic#or(Object, Object)
     */
    OR("|", "or", 2),

    /**
     * Bitwise-xor operator.
     * <br><strong>Syntax:</strong> {@code x ^ y}
     * <br><strong>Method:</strong> {@code T xor(L x, R y);}.
     * @see JexlArithmetic#xor(Object, Object)
     */
    XOR("^", "xor", 2),

    /**
     * Bit-pattern right-shift operator.
     * <br><strong>Syntax:</strong> {@code x >> y}
     * <br><strong>Method:</strong> {@code T rightShift(L x, R y);}.
     * @see JexlArithmetic#shiftRight(Object, Object)
     */
    SHIFTRIGHT(">>", "shiftRight", 2),

    /**
     * Bit-pattern right-shift unsigned operator.
     * <br><strong>Syntax:</strong> {@code x >>> y}
     * <br><strong>Method:</strong> {@code T rightShiftUnsigned(L x, R y);}.
     * @see JexlArithmetic#shiftRightUnsigned(Object, Object)
     */
    SHIFTRIGHTU(">>>", "shiftRightUnsigned", 2),

    /**
     * Bit-pattern left-shift operator.
     * <br><strong>Syntax:</strong> {@code x << y}
     * <br><strong>Method:</strong> {@code T leftShift(L x, R y);}.
     * @see JexlArithmetic#shiftLeft(Object, Object)
     */
    SHIFTLEFT("<<", "shiftLeft", 2),

    /**
     * Equals operator.
     * <br><strong>Syntax:</strong> {@code x == y}
     * <br><strong>Method:</strong> {@code boolean equals(L x, R y);}.
     * @see JexlArithmetic#equals(Object, Object)
     */
    EQ("==", "equals", 2),

    /**
     * Equal-strict operator.
     * <br><strong>Syntax:</strong> {@code x === y}
     * <br><strong>Method:</strong> {@code boolean strictEquals(L x, R y);}.
     * @see JexlArithmetic#strictEquals(Object, Object)
     */
    EQSTRICT("===", "strictEquals", 2),

    /**
     * Less-than operator.
     * <br><strong>Syntax:</strong> {@code x < y}
     * <br><strong>Method:</strong> {@code boolean lessThan(L x, R y);}.
     * @see JexlArithmetic#lessThan(Object, Object)
     */
    LT("<", "lessThan", 2),

    /**
     * Less-than-or-equal operator.
     * <br><strong>Syntax:</strong> {@code x <= y}
     * <br><strong>Method:</strong> {@code boolean lessThanOrEqual(L x, R y);}.
     * @see JexlArithmetic#lessThanOrEqual(Object, Object)
     */
    LTE("<=", "lessThanOrEqual", 2),

    /**
     * Greater-than operator.
     * <br><strong>Syntax:</strong> {@code x > y}
     * <br><strong>Method:</strong> {@code boolean greaterThan(L x, R y);}.
     * @see JexlArithmetic#greaterThan(Object, Object)
     */
    GT(">", "greaterThan", 2),

    /**
     * Greater-than-or-equal operator.
     * <br><strong>Syntax:</strong> {@code x >= y}
     * <br><strong>Method:</strong> {@code boolean greaterThanOrEqual(L x, R y);}.
     * @see JexlArithmetic#greaterThanOrEqual(Object, Object)
     */
    GTE(">=", "greaterThanOrEqual", 2),

    /**
     * Contains operator.
     * <br><strong>Syntax:</strong> {@code x =~ y}
     * <br><strong>Method:</strong> {@code boolean contains(L x, R y);}.
     * @see JexlArithmetic#contains(Object, Object)
     */
    CONTAINS("=~", "contains", 2),

    /**
     * Starts-with operator.
     * <br><strong>Syntax:</strong> {@code x =^ y}
     * <br><strong>Method:</strong> {@code boolean startsWith(L x, R y);}.
     * @see JexlArithmetic#startsWith(Object, Object)
     */
    STARTSWITH("=^", "startsWith", 2),

    /**
     * Ends-with operator.
     * <br><strong>Syntax:</strong> {@code x =$ y}
     * <br><strong>Method:</strong> {@code boolean endsWith(L x, R y);}.
     * @see JexlArithmetic#endsWith(Object, Object)
     */
    ENDSWITH("=$", "endsWith", 2),

    /**
     * Not operator.
     * <br><strong>Syntax:</strong> {@code !x}
     * <br><strong>Method:</strong> {@code T not(L x);}.
     * @see JexlArithmetic#not(Object)
     */
    NOT("!", "not", 1),

    /**
     * Complement operator.
     * <br><strong>Syntax:</strong> {@code ~x}
     * <br><strong>Method:</strong> {@code T complement(L x);}.
     * @see JexlArithmetic#complement(Object)
     */
    COMPLEMENT("~", "complement", 1),

    /**
     * Negate operator.
     * <br><strong>Syntax:</strong> {@code -x}
     * <br><strong>Method:</strong> {@code T negate(L x);}.
     * @see JexlArithmetic#negate(Object)
     */
    NEGATE("-", "negate", 1),

    /**
     * Positivize operator.
     * <br><strong>Syntax:</strong> {@code +x}
     * <br><strong>Method:</strong> {@code T positivize(L x);}.
     * @see JexlArithmetic#positivize(Object)
     */
    POSITIVIZE("+", "positivize", 1),

    /**
     * Empty operator.
     * <br><strong>Syntax:</strong> {@code empty x} or {@code empty(x)}
     * <br><strong>Method:</strong> {@code boolean empty(L x);}.
     * @see JexlArithmetic#empty(Object)
     */
    EMPTY("empty", "empty", 1),

    /**
     * Size operator.
     * <br><strong>Syntax:</strong> {@code size x} or {@code size(x)}
     * <br><strong>Method:</strong> {@code int size(L x);}.
     * @see JexlArithmetic#size(Object)
     */
    SIZE("size", "size", 1),

    /**
     * Self-add operator.
     * <br><strong>Syntax:</strong> {@code x += y}
     * <br><strong>Method:</strong> {@code T selfAdd(L x, R y);}.
     */
    SELF_ADD("+=", "selfAdd", ADD),

    /**
     * Self-subtract operator.
     * <br><strong>Syntax:</strong> {@code x -= y}
     * <br><strong>Method:</strong> {@code T selfSubtract(L x, R y);}.
     */
    SELF_SUBTRACT("-=", "selfSubtract", SUBTRACT),

    /**
     * Self-multiply operator.
     * <br><strong>Syntax:</strong> {@code x *= y}
     * <br><strong>Method:</strong> {@code T selfMultiply(L x, R y);}.
     */
    SELF_MULTIPLY("*=", "selfMultiply", MULTIPLY),

    /**
     * Self-divide operator.
     * <br><strong>Syntax:</strong> {@code x /= y}
     * <br><strong>Method:</strong> {@code T selfDivide(L x, R y);}.
     */
    SELF_DIVIDE("/=", "selfDivide", DIVIDE),

    /**
     * Self-modulo operator.
     * <br><strong>Syntax:</strong> {@code x %= y}
     * <br><strong>Method:</strong> {@code T selfMod(L x, R y);}.
     */
    SELF_MOD("%=", "selfMod", MOD),

    /**
     * Self-and operator.
     * <br><strong>Syntax:</strong> {@code x &amp;= y}
     * <br><strong>Method:</strong> {@code T selfAnd(L x, R y);}.
     */
    SELF_AND("&=", "selfAnd", AND),

    /**
     * Self-or operator.
     * <br><strong>Syntax:</strong> {@code x |= y}
     * <br><strong>Method:</strong> {@code T selfOr(L x, R y);}.
     */
    SELF_OR("|=", "selfOr", OR),

    /**
     * Self-xor operator.
     * <br><strong>Syntax:</strong> {@code x ^= y}
     * <br><strong>Method:</strong> {@code T selfXor(L x, R y);}.
     */
    SELF_XOR("^=", "selfXor", XOR),

    /**
     * Self-right-shift operator.
     * <br><strong>Syntax:</strong> {@code x >>= y}
     * <br><strong>Method:</strong> {@code T selfShiftRight(L x, R y);}.
     */
    SELF_SHIFTRIGHT(">>=", "selfShiftRight", SHIFTRIGHT),

    /**
     * Self-right-shift unsigned operator.
     * <br><strong>Syntax:</strong> {@code x >>> y}
     * <br><strong>Method:</strong> {@code T selfShiftRightUnsigned(L x, R y);}.
     */
    SELF_SHIFTRIGHTU(">>>=", "selfShiftRightUnsigned", SHIFTRIGHTU),

    /**
     * Self-left-shift operator.
     * <br><strong>Syntax:</strong> {@code x << y}
     * <br><strong>Method:</strong> {@code T selfShiftLeft(L x, R y);}.
     */
    SELF_SHIFTLEFT("<<=", "selfShiftLeft", SHIFTLEFT),

    /**
     * Increment pseudo-operator.
     * <br>No syntax, used as helper for the prefix and postfix versions of {@code ++}.
     * @see JexlArithmetic#increment(Object)
     */
    INCREMENT("+1", "increment", 1),

    /**
     * Decrement pseudo-operator.
     * <br>No syntax, used as helper for the prefix and postfix versions of {@code --}.
     * @see JexlArithmetic#decrement(Object)
     */
    DECREMENT("-1", "decrement", 1),

    /**
     * Prefix ++ operator, increments and returns the value after incrementing.
     * <br><strong>Syntax:</strong> {@code ++x}
     * <br><strong>Method:</strong> {@code T incrementAndGet(L x);}.
     */
    INCREMENT_AND_GET("++.", "incrementAndGet", INCREMENT, 1),

    /**
     * Postfix ++, increments and returns the value before incrementing.
     * <br><strong>Syntax:</strong> {@code x++}
     * <br><strong>Method:</strong> {@code T getAndIncrement(L x);}.
     */
    GET_AND_INCREMENT(".++", "getAndIncrement", INCREMENT, 1),

    /**
     * Prefix --, decrements and returns the value after decrementing.
     * <br><strong>Syntax:</strong> {@code --x}
     * <br><strong>Method:</strong> {@code T decrementAndGet(L x);}.
     */
    DECREMENT_AND_GET("--.", "decrementAndGet", DECREMENT, 1),

    /**
     * Postfix --, decrements and returns the value before decrementing.
     * <br><strong>Syntax:</strong> {@code x--}
     * <br><strong>Method:</strong> {@code T getAndDecrement(L x);}.
     */
    GET_AND_DECREMENT(".--", "getAndDecrement", DECREMENT, 1),

    /**
     * Marker for side effect.
     * <br>Returns this from 'self*' overload method to let the engine know the side effect has been performed and
     * there is no need to assign the result.
     * @deprecated 3.4.1
     */
    ASSIGN("=", null, null),

    /**
     * Property get operator as in: x.y.
     * <br><strong>Syntax:</strong> {@code x.y}
     * <br><strong>Method:</strong> {@code Object propertyGet(L x, R y);}.
     */
    PROPERTY_GET(".", "propertyGet", 2),

    /**
     * Property set operator as in: x.y = z.
     * <br><strong>Syntax:</strong> {@code x.y = z}
     * <br><strong>Method:</strong> {@code void propertySet(L x, R y, V z);}.
     */
    PROPERTY_SET(".=", "propertySet", 3),

    /**
     * Array get operator as in: x[y].
     * <br><strong>Syntax:</strong> {@code x.y}
     * <br><strong>Method:</strong> {@code Object arrayGet(L x, R y);}.
     */
    ARRAY_GET("[]", "arrayGet", 2),

    /**
     * Array set operator as in: x[y] = z.
     * <br><strong>Syntax:</strong> {@code x[y] = z}
     * <br><strong>Method:</strong> {@code void arraySet(L x, R y, V z);}.
     */
    ARRAY_SET("[]=", "arraySet", 3),

    /**
     * Iterator generator as in for(var x : y).
     * If the returned Iterator is AutoCloseable, close will be called after the last execution of the loop block.
     * <br><strong>Syntax:</strong> <code>for(var x : y){...}</code>
     * <br><strong>Method:</strong> {@code Iterator<Object> forEach(R y);}.
     * @since 3.1
     */
    FOR_EACH("for(...)", "forEach", 1),

    /**
     * Test condition in if, for, while.
     * <br><strong>Method:</strong> {@code boolean testCondition(R y);}.
     * @since 3.3
     */
    CONDITION("?", "testCondition", 1),

    /**
     * Compare overload as in compare(x, y).
     * <br><strong>Method:</strong> {@code boolean compare(L x, R y);}.
     * @since 3.4.1
     */
    COMPARE("<>", "compare", 2),

    /**
     * Not-Contains operator.
     * <p>Not overridable, calls !(contain(...))</p>
     */
    NOT_CONTAINS("!~", null, CONTAINS),

    /**
     * Not-Starts-With operator.
     * <p>Not overridable, calls !(startsWith(...))</p>
     */
    NOT_STARTSWITH("!^", null, STARTSWITH),

    /**
     * Not-Ends-With operator.
     * <p>Not overridable, calls !(endsWith(...))</p>
     */
    NOT_ENDSWITH("!$", null, ENDSWITH),;

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
    JexlOperator(final String o, final String m, final int argc) {
        this(o, m, null, argc);
    }

    /**
     * Creates a side effect operator with arity == 2.
     *
     * @param o the operator name
     * @param m the method name associated to this operator in a JexlArithmetic
     * @param b the base operator, ie + for +=
     */
    JexlOperator(final String o, final String m, final JexlOperator b) {
        this(o, m, b, 2);
    }

    /**
     * Creates a side effect operator.
     *
     * @param o the operator name
     * @param m the method name associated to this operator in a JexlArithmetic
     * @param b the base operator, ie + for +=
     * @param a the operator arity
     */
    JexlOperator(final String o, final String m, final JexlOperator b, final int a) {
        this.operator = o;
        this.methodName = m;
        this.arity = a;
        this.base = b;
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

    /**
     * Gets this operator method name in a JexlArithmetic.
     *
     * @return the method name
     */
    public final String getMethodName() {
        return methodName;
    }

    /**
     * Gets this operator symbol.
     *
     * @return the symbol
     */
    public final String getOperatorSymbol() {
        return operator;
    }

}
