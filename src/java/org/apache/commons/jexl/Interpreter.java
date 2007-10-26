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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.jexl.parser.ASTAddNode;
import org.apache.commons.jexl.parser.ASTAndNode;
import org.apache.commons.jexl.parser.ASTArrayAccess;
import org.apache.commons.jexl.parser.ASTAssignment;
import org.apache.commons.jexl.parser.ASTBitwiseAndNode;
import org.apache.commons.jexl.parser.ASTBitwiseComplNode;
import org.apache.commons.jexl.parser.ASTBitwiseOrNode;
import org.apache.commons.jexl.parser.ASTBitwiseXorNode;
import org.apache.commons.jexl.parser.ASTBlock;
import org.apache.commons.jexl.parser.ASTDivNode;
import org.apache.commons.jexl.parser.ASTEQNode;
import org.apache.commons.jexl.parser.ASTEmptyFunction;
import org.apache.commons.jexl.parser.ASTExpression;
import org.apache.commons.jexl.parser.ASTExpressionExpression;
import org.apache.commons.jexl.parser.ASTFalseNode;
import org.apache.commons.jexl.parser.ASTFloatLiteral;
import org.apache.commons.jexl.parser.ASTForeachStatement;
import org.apache.commons.jexl.parser.ASTGENode;
import org.apache.commons.jexl.parser.ASTGTNode;
import org.apache.commons.jexl.parser.ASTIdentifier;
import org.apache.commons.jexl.parser.ASTIfStatement;
import org.apache.commons.jexl.parser.ASTIntegerLiteral;
import org.apache.commons.jexl.parser.ASTJexlScript;
import org.apache.commons.jexl.parser.ASTLENode;
import org.apache.commons.jexl.parser.ASTLTNode;
import org.apache.commons.jexl.parser.ASTMapEntry;
import org.apache.commons.jexl.parser.ASTMapLiteral;
import org.apache.commons.jexl.parser.ASTMethod;
import org.apache.commons.jexl.parser.ASTModNode;
import org.apache.commons.jexl.parser.ASTMulNode;
import org.apache.commons.jexl.parser.ASTNENode;
import org.apache.commons.jexl.parser.ASTNotNode;
import org.apache.commons.jexl.parser.ASTNullLiteral;
import org.apache.commons.jexl.parser.ASTOrNode;
import org.apache.commons.jexl.parser.ASTReference;
import org.apache.commons.jexl.parser.ASTReferenceExpression;
import org.apache.commons.jexl.parser.ASTSizeFunction;
import org.apache.commons.jexl.parser.ASTSizeMethod;
import org.apache.commons.jexl.parser.ASTStatementExpression;
import org.apache.commons.jexl.parser.ASTStringLiteral;
import org.apache.commons.jexl.parser.ASTSubtractNode;
import org.apache.commons.jexl.parser.ASTTrueNode;
import org.apache.commons.jexl.parser.ASTUnaryMinusNode;
import org.apache.commons.jexl.parser.ASTWhileStatement;
import org.apache.commons.jexl.parser.Node;
import org.apache.commons.jexl.parser.SimpleNode;
import org.apache.commons.jexl.parser.VisitorAdapter;
import org.apache.commons.jexl.util.Coercion;
import org.apache.commons.jexl.util.Introspector;
import org.apache.commons.jexl.util.introspection.Info;
import org.apache.commons.jexl.util.introspection.Uberspect;
import org.apache.commons.jexl.util.introspection.VelMethod;
import org.apache.commons.jexl.util.introspection.VelPropertyGet;

/**
 * Starting point for an interpreter of JEXL syntax.
 */
class Interpreter extends VisitorAdapter {

    /** The uberspect. */
    private Uberspect uberspect;

    /** The context to store/retrieve variables. */
    private JexlContext context;

    /** dummy velocity info. */
    private static final Info DUMMY = new Info("", 1, 1);

    /**
     * Create the interpreter with the default settings.
     */
    public Interpreter() {
        super();
        setUberspect(Introspector.getUberspect());
    }

    /**
     * Interpret the given script/expression.
     * 
     * @param node the script or expression to interpret.
     * @param aContext the context to interpret against.
     * @return the result of the interpretation.
     */
    public Object interpret(SimpleNode node, JexlContext aContext) {
        setContext(aContext);
        return node.jjtAccept(this, null);
    }

    /**
     * TODO: Does this need to be a setter.
     * sets the uberspect to use for divining bean properties etc.
     * 
     * @param anUberspect the uberspect.
     */
    public void setUberspect(Uberspect anUberspect) {
        uberspect = anUberspect;
    }

    /**
     * Gets the uberspect.
     * 
     * @return an {@link Uberspect}
     */
    protected Uberspect getUberspect() {
        return uberspect;
    }

    /**
     * Sets the context that contain variables.
     * 
     * @param aContext a {link JexlContext}
     */
    public void setContext(JexlContext aContext) {
        context = aContext;
    }

    // up to here
    /** {@inheritDoc} */
    public Object visit(ASTAddNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);

        // the spec says 'and'
        if (left == null && right == null) {
            return new Long(0);
        }

        if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {

            // in the event that either is null and not both, then just make the
            // null a 0
            try {
                double l = Coercion.coercedouble(left);
                double r = Coercion.coercedouble(right);
                return new Double(l + r);
            } catch (java.lang.NumberFormatException nfe) {
                // Well, use strings!
                return left.toString().concat(right.toString());
            }
        }

        // TODO: support BigDecimal/BigInteger too

        // attempt to use Longs
        try {
            long l = Coercion.coercelong(left);
            long r = Coercion.coercelong(right);
            return new Long(l + r);
        } catch (java.lang.NumberFormatException nfe) {
            // Well, use strings!
            return left.toString().concat(right.toString());
        }
    }

    /** {@inheritDoc} */
    public Object visit(ASTAndNode node, Object data) {

        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        boolean leftValue = Coercion.coerceboolean(left);

        // coercion rules
        return (leftValue && Coercion.coerceboolean(node.jjtGetChild(1).jjtAccept(this, data))) ? Boolean.TRUE
            : Boolean.FALSE;
    }

    /** {@inheritDoc} */
    public Object visit(ASTArrayAccess node, Object data) {
        // first child is the identifier
        Object object = node.jjtGetChild(0).jjtAccept(this, data);
        // can have multiple nodes - either an expression, integer literal or
        // reference
        int numChildren = node.jjtGetNumChildren();
        for (int i = 1; i < numChildren; i++) {
            Object index = node.jjtGetChild(i).jjtAccept(this, null);
            object = getAttribute(object, index);
        }

        return object;
    }

    /** {@inheritDoc} */
    public Object visit(ASTAssignment node, Object data) {
        // child 0 should be the variable (reference) to assign to
        Node left = node.jjtGetChild(0);
        Object result = null;
        if (left instanceof ASTReference) {
            ASTReference reference = (ASTReference) left;
            left = reference.jjtGetChild(0);
            // TODO: this only works for a Reference that has a single
            // identifier as it's child
            if (left instanceof ASTIdentifier) {
                String identifier = ((ASTIdentifier) left).image;
                result = node.jjtGetChild(1).jjtAccept(this, data);
                context.getVars().put(identifier, result);
            }
        } else {
            throw new RuntimeException("Trying to assign to something other than a reference: " + left);
        }

        return result;
    }

    /** {@inheritDoc} */
    public Object visit(ASTBitwiseAndNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        // coerce these two values longs and add.
        long l = Coercion.coercelong(left);
        long r = Coercion.coercelong(right);
        return new Long(l & r);
    }

    /** {@inheritDoc} */
    public Object visit(ASTBitwiseComplNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        long l = Coercion.coercelong(left);
        return new Long(~l);
    }

    /** {@inheritDoc} */
    public Object visit(ASTBitwiseOrNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        long l = Coercion.coercelong(left);
        long r = Coercion.coercelong(right);
        return new Long(l | r);
    }

    /** {@inheritDoc} */
    public Object visit(ASTBitwiseXorNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        long l = Coercion.coercelong(left);
        long r = Coercion.coercelong(right);
        return new Long(l ^ r);
    }

    /** {@inheritDoc} */
    public Object visit(ASTBlock node, Object data) {
        int numChildren = node.jjtGetNumChildren();
        Object result = null;
        for (int i = 0; i < numChildren; i++) {
            result = node.jjtGetChild(i).jjtAccept(this, data);
        }
        return result;
    }

    /** {@inheritDoc} */
    public Object visit(ASTDivNode node, Object data) {

        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);

        // the spec says 'and', I think 'or'
        if (left == null && right == null) {
            return new Byte((byte) 0);
        }

        Double l = Coercion.coerceDouble(left);
        Double r = Coercion.coerceDouble(right);

        // catch div/0
        if (r.doubleValue() == 0.0) {
            return r;
        }

        return new Double(l.doubleValue() / r.doubleValue());
    }

    /** {@inheritDoc} */
    public Object visit(ASTEmptyFunction node, Object data) {

        Object o = node.jjtGetChild(0).jjtAccept(this, data);

        if (o == null) {
            return Boolean.TRUE;
        }

        if (o instanceof String && "".equals(o)) {
            return Boolean.TRUE;
        }

        if (o.getClass().isArray() && ((Object[]) o).length == 0) {
            return Boolean.TRUE;
        }

        if (o instanceof Collection && ((Collection) o).isEmpty()) {
            return Boolean.TRUE;
        }

        // Map isn't a collection
        if (o instanceof Map && ((Map) o).isEmpty()) {
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    /** {@inheritDoc} */
    public Object visit(ASTEQNode node, Object data) {

        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);

        return equals(left, right) ? Boolean.TRUE : Boolean.FALSE;
    }

    /** {@inheritDoc} */
    public Object visit(ASTExpression node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTExpressionExpression node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTFalseNode node, Object data) {

        return Boolean.FALSE;
    }

    /** {@inheritDoc} */
    public Object visit(ASTFloatLiteral node, Object data) {

        return Float.valueOf(node.image);
    }

    /** {@inheritDoc} */
    public Object visit(ASTForeachStatement node, Object data) {

        Object result = null;
        /* first child is the loop variable */
        ASTReference loopReference = (ASTReference) node.jjtGetChild(0);
        ASTIdentifier loopVariable = (ASTIdentifier) loopReference.jjtGetChild(0);
        /* second child is the variable to iterate */
        Object iterableValue = node.jjtGetChild(1).jjtAccept(this, data);
        // make sure there is a value to iterate on and a statement to execute
        if (iterableValue != null && node.jjtGetNumChildren() >= 3) {
            /* third child is the statement to execute */
            SimpleNode statement = (SimpleNode) node.jjtGetChild(2);
            // get an iterator for the collection/array etc via the
            // introspector.
            Iterator itemsIterator = getUberspect().getIterator(iterableValue, DUMMY);
            while (itemsIterator.hasNext()) {
                // set loopVariable to value of iterator
                Object value = itemsIterator.next();
                context.getVars().put(loopVariable.image, value);
                // execute statement
                result = statement.jjtAccept(this, data);
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    public Object visit(ASTGENode node, Object data) {

        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);

        return greaterThanOrEqual(left, right) ? Boolean.TRUE : Boolean.FALSE;
    }

    /** {@inheritDoc} */
    public Object visit(ASTGTNode node, Object data) {

        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);

        return greaterThan(left, right) ? Boolean.TRUE : Boolean.FALSE;
    }

    /** {@inheritDoc} */
    public Object visit(ASTIdentifier node, Object data) {

        String name = node.image;
        if (data == null) {
            return context.getVars().get(name);
        } else {
            return getAttribute(data, name);
        }
    }

    /** {@inheritDoc} */
    public Object visit(ASTIfStatement node, Object data) {

        Object result = null;
        /* first child is the expression */
        Object expression = node.jjtGetChild(0).jjtAccept(this, data);
        if (Coercion.coerceboolean(expression)) {
            // first child is true statement
            result = node.jjtGetChild(1).jjtAccept(this, data);
        } else {
            // if there is a false, execute it. false statement is the second
            // child
            if (node.jjtGetNumChildren() == 3) {
                result = node.jjtGetChild(2).jjtAccept(this, data);
            }
        }

        return result;
    }

    /** {@inheritDoc} */
    public Object visit(ASTIntegerLiteral node, Object data) {
        Integer value = Integer.valueOf(node.image);
        if (data == null) {
            return value;
        } else {
            return getAttribute(data, value);
        }
    }

    /** {@inheritDoc} */
    public Object visit(ASTJexlScript node, Object data) {

        int numChildren = node.jjtGetNumChildren();
        Object result = null;
        for (int i = 0; i < numChildren; i++) {
            Node child = node.jjtGetChild(i);
            result = child.jjtAccept(this, data);
        }
        return result;
    }

    /** {@inheritDoc} */
    public Object visit(ASTLENode node, Object data) {

        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);

        return lessThanOrEqual(left, right) ? Boolean.TRUE : Boolean.FALSE;
    }

    /** {@inheritDoc} */
    public Object visit(ASTLTNode node, Object data) {

        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);

        return lessThan(left, right) ? Boolean.TRUE : Boolean.FALSE;
    }

    /** {@inheritDoc} */
    public Object visit(ASTMapEntry node, Object data) {

        return new Object[] { 
            (node.jjtGetChild(0)).jjtAccept(this, data), 
            (node.jjtGetChild(1)).jjtAccept(this, data) 
        };
    }

    /** {@inheritDoc} */
    public Object visit(ASTMapLiteral node, Object data) {

        int childCount = node.jjtGetNumChildren();
        Map map = new HashMap();

        for (int i = 0; i < childCount; i++) {
            Object[] entry = (Object[]) (node.jjtGetChild(i)).jjtAccept(this, data);
            map.put(entry[0], entry[1]);
        }

        return map;
    }

    /** {@inheritDoc} */
    public Object visit(ASTMethod node, Object data) {

        // child 0 is the identifier (method name), the others are parameters.
        // the object to invoke the method on should be in the data argument
        String methodName = ((ASTIdentifier) node.jjtGetChild(0)).image;

        int paramCount = node.jjtGetNumChildren() - 1;

        // get our params
        Object[] params = new Object[paramCount];

        try {
            for (int i = 0; i < paramCount; i++) {
                params[i] = node.jjtGetChild(i + 1).jjtAccept(this, null);
            }

            VelMethod vm = getUberspect().getMethod(data, methodName, params, DUMMY);
            // DG: If we can't find an exact match, narrow the parameters and
            // try again!
            if (vm == null) {

                // replace all numbers with the smallest type that will fit
                for (int i = 0; i < params.length; i++) {
                    Object param = params[i];
                    if (param instanceof Number) {
                        params[i] = narrow((Number) param);
                    }
                }
                vm = getUberspect().getMethod(data, methodName, params, DUMMY);
                if (vm == null) {
                    return null;
                }
            }

            return vm.invoke(data, params);
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();

            if (t instanceof Exception) {
                throw new RuntimeException(t);
            }

            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    public Object visit(ASTModNode node, Object data) {

        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);

        // the spec says 'and', I think 'or'
        if (left == null && right == null) {
            return new Byte((byte) 0);
        }

        // if anything is float, double or string with ( "." | "E" | "e") coerce
        // all to doubles and do it
        if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
            Double l = Coercion.coerceDouble(left);
            Double r = Coercion.coerceDouble(right);

            // catch div/0
            if (r.doubleValue() == 0.0) {
                return r;
            }

            return new Double(l.doubleValue() % r.doubleValue());
        }

        // otherwise to longs with thee!

        long l = Coercion.coercelong(left);
        long r = Coercion.coercelong(right);

        // catch the div/0
        if (r == 0) {
            return new Long(0);
        }

        return new Long(l % r);
    }

    /** {@inheritDoc} */
    public Object visit(ASTMulNode node, Object data) {

        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);

        // the spec says 'and', I think 'or'
        if (left == null && right == null) {
            return new Byte((byte) 0);
        }

        // if anything is float, double or string with ( "." | "E" | "e") coerce
        // all to doubles and do it
        if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
            Double l = Coercion.coerceDouble(left);
            Double r = Coercion.coerceDouble(right);

            return new Double(l.doubleValue() * r.doubleValue());
        }

        // otherwise to longs with thee!

        long l = Coercion.coercelong(left);
        long r = Coercion.coercelong(right);

        return new Long(l * r);
    }

    /** {@inheritDoc} */
    public Object visit(ASTNENode node, Object data) {

        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);

        return equals(left, right) ? Boolean.FALSE : Boolean.TRUE;
    }

    /** {@inheritDoc} */
    public Object visit(ASTNotNode node, Object data) {

        Object val = node.jjtGetChild(0).jjtAccept(this, data);

        // coercion rules
        if (val != null) {
            return Coercion.coerceboolean(val) ? Boolean.FALSE : Boolean.TRUE;
        }

        throw new IllegalArgumentException("not expression: not boolean valued " + val);
    }

    /** {@inheritDoc} */
    public Object visit(ASTNullLiteral node, Object data) {
        return null;
    }

    /** {@inheritDoc} */
    public Object visit(ASTOrNode node, Object data) {

        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        boolean leftValue = Coercion.coerceboolean(left);

        // coercion rules
        return (leftValue || Coercion.coerceboolean(node.jjtGetChild(1).jjtAccept(this, data))) ? Boolean.TRUE
            : Boolean.FALSE;
    }

    /** {@inheritDoc} */
    public Object visit(ASTReference node, Object data) {
        // could be array access, identifier or map literal
        // followed by zero or more ("." and array access, method, size,
        // identifier or integer literal)

        int numChildren = node.jjtGetNumChildren();

        // pass first piece of data in and loop through children
        Object result = null;
        StringBuffer variableName = new StringBuffer();
        boolean isVariable = true;
        for (int i = 0; i < numChildren; i++) {
            Node theNode = node.jjtGetChild(i);
            isVariable = isVariable && (theNode instanceof ASTIdentifier);
            result = theNode.jjtAccept(this, result);
            // if we get null back a result, check for an ant variable
            if (result == null && isVariable) {
                if (i != 0) {
                    variableName.append('.');
                }
                String name = ((ASTIdentifier) theNode).image;
                variableName.append(name);
                result = context.getVars().get(variableName.toString());
            }
        }

        return result;
    }

    /** {@inheritDoc} */
    public Object visit(ASTReferenceExpression node, Object data) {

        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTSizeFunction node, Object data) {

        Object val = node.jjtGetChild(0).jjtAccept(this, data);

        if (val == null) {
            throw new IllegalArgumentException("size() : null arg");
        }

        return new Integer(sizeOf(val));
    }

    /** {@inheritDoc} */
    public Object visit(ASTSizeMethod node, Object data) {

        return new Integer(sizeOf(data));
    }

    /** {@inheritDoc} */
    public Object visit(ASTStatementExpression node, Object data) {

        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTStringLiteral node, Object data) {
        return node.image;
    }

    /** {@inheritDoc} */
    public Object visit(ASTSubtractNode node, Object data) {

        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);

        // the spec says 'and', I think 'or'
        if (left == null && right == null) {
            return new Byte((byte) 0);
        }

        // if anything is float, double or string with ( "." | "E" | "e") coerce
        // all to doubles and do it
        if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {

            // in the event that either is null and not both, then just make the
            // null a 0
            double l = Coercion.coercedouble(left);
            double r = Coercion.coercedouble(right);
            return new Double(l - r);
        }

        // otherwise to longs with thee!

        long l = Coercion.coercelong(left);
        long r = Coercion.coercelong(right);

        return new Long(l - r);
    }

    /** {@inheritDoc} */
    public Object visit(ASTTrueNode node, Object data) {
        return Boolean.TRUE;
    }

    /** {@inheritDoc} */
    public Object visit(ASTUnaryMinusNode node, Object data) {

        Object val = node.jjtGetChild(0).jjtAccept(this, data);

        if (val instanceof Byte) {
            byte valueAsByte = ((Byte) val).byteValue();
            return new Byte((byte) -valueAsByte);
        } else if (val instanceof Short) {
            short valueAsShort = ((Short) val).shortValue();
            return new Short((short) -valueAsShort);
        } else if (val instanceof Integer) {
            int valueAsInt = ((Integer) val).intValue();
            return new Integer(-valueAsInt);
        } else if (val instanceof Long) {
            long valueAsLong = ((Long) val).longValue();
            return new Long(-valueAsLong);
        } else if (val instanceof Float) {
            float valueAsFloat = ((Float) val).floatValue();
            return new Float(-valueAsFloat);
        } else if (val instanceof Double) {
            double valueAsDouble = ((Double) val).doubleValue();
            return new Double(-valueAsDouble);
        } else if (val instanceof BigDecimal) {
            BigDecimal valueAsBigD = (BigDecimal) val;
            return valueAsBigD.negate();
        } else if (val instanceof BigInteger) {
            BigInteger valueAsBigI = (BigInteger) val;
            return valueAsBigI.negate();
        }
        throw new NumberFormatException("expression not a number: " + val);
    }

    /** {@inheritDoc} */
    public Object visit(ASTWhileStatement node, Object data) {

        Object result = null;
        /* first child is the expression */
        Node expressionNode = (Node) node.jjtGetChild(0);
        while (Coercion.coerceboolean(expressionNode.jjtAccept(this, data))) {
            // execute statement
            result = node.jjtGetChild(1).jjtAccept(this, data);
        }

        return result;
    }

    // other stuff

    /**
     * Given a Number, return back the value using the smallest type the result
     * will fit into. This works hand in hand with parameter 'widening' in java
     * method calls, e.g. a call to substring(int,int) with an int and a long
     * will fail, but a call to substring(int,int) with an int and a short will
     * succeed.
     * 
     * @param original the original number.
     * @return a value of the smallest type the original number will fit into.
     * @since 1.1
     */
    private Number narrow(Number original) {
        if (original == null || original instanceof BigDecimal || original instanceof BigInteger) {
            return original;
        }
        Number result = original;
        if (original instanceof Double || original instanceof Float) {
            double value = original.doubleValue();
            if (value <= Float.MAX_VALUE && value >= Float.MIN_VALUE) {
                result = new Float(result.floatValue());
            }
            // else it was already a double
        } else {
            long value = original.longValue();
            if (value <= Byte.MAX_VALUE && value >= Byte.MIN_VALUE) {
                // it will fit in a byte
                result = new Byte((byte) value);
            } else if (value <= Short.MAX_VALUE && value >= Short.MIN_VALUE) {
                result = new Short((short) value);
            } else if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
                result = new Integer((int) value);
            }
            // else it was already a long
        }
        return result;
    }

    /**
     * Calculate the <code>size</code> of various types: Collection, Array,
     * Map, String, and anything that has a int size() method.
     * 
     * @param val the object to get the size of.
     * @return the size of val
     */
    private int sizeOf(Object val) {
        if (val instanceof Collection) {
            return ((Collection) val).size();
        } else if (val.getClass().isArray()) {
            return Array.getLength(val);
        } else if (val instanceof Map) {
            return ((Map) val).size();
        } else if (val instanceof String) {
            return ((String) val).length();
        } else {
            // check if there is a size method on the object that returns an
            // integer
            // and if so, just use it
            Object[] params = new Object[0];
            // Info velInfo = new Info("", 1, 1);
            VelMethod vm = uberspect.getMethod(val, "size", params, DUMMY);
            if (vm != null && vm.getReturnType() == Integer.TYPE) {
                Integer result;
                try {
                    result = (Integer) vm.invoke(val, params);
                } catch (Exception e) {
                    throw new RuntimeException("size() : error executing", e);
                }
                return result.intValue();
            }
            throw new IllegalArgumentException("size() : unknown type : " + val.getClass());
        }
    }

    /**
     * Test if the passed value is a floating point number, i.e. a float, double
     * or string with ( "." | "E" | "e").
     * 
     * @param val the object to be tested
     * @return true if it is, false otherwise.
     */
    private boolean isFloatingPointNumber(Object val) {
        if (val instanceof Float || val instanceof Double) {
            return true;
        }
        if (val instanceof String) {
            String string = (String) val;
            return string.indexOf(".") != -1 || string.indexOf("e") != -1 || string.indexOf("E") != -1;
        }
        return false;
    }

    /**
     * Get an attribute of an object.
     * 
     * @param object to retrieve value from
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or
     *            key for a map
     * @return the attribute.
     */
    private Object getAttribute(Object object, Object attribute) {
        if (object == null) {
            return null;
        }
        if (attribute == null) {
            return null;
        }
        if (object instanceof Map) {
            return ((Map) object).get(attribute);
        } else if (object instanceof List) {
            int idx = Coercion.coerceinteger(attribute);

            try {
                return ((List) object).get(idx);
            } catch (IndexOutOfBoundsException iobe) {
                return null;
            }
        } else if (object.getClass().isArray()) {
            int idx = Coercion.coerceinteger(attribute);

            try {
                return Array.get(object, idx);
            } catch (ArrayIndexOutOfBoundsException aiobe) {
                return null;
            }
        } else {
            // look up bean property of data and return
            VelPropertyGet vg = getUberspect().getPropertyGet(object, attribute.toString(), DUMMY);

            if (vg != null) {
                try {
                    return vg.invoke(object);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                return null;
            }

        }
    }

    /**
     * Test if left and right are equal.
     * 
     * @param left first value
     * @param right second value
     * @return test result.
     */
    private boolean equals(Object left, Object right) {
        if (left == null && right == null) {
            /*
             * if both are null L == R
             */
            return true;
        } else if (left == null || right == null) {
            /*
             * we know both aren't null, therefore L != R
             */
            return false;
        } else if (left.getClass().equals(right.getClass())) {
            return left.equals(right);
        } else if (left instanceof Float || left instanceof Double 
            || right instanceof Float || right instanceof Double) {
            Double l = Coercion.coerceDouble(left);
            Double r = Coercion.coerceDouble(right);

            return l.equals(r);
        } else if (left instanceof Number || right instanceof Number || left instanceof Character
            || right instanceof Character) {
            return Coercion.coerceLong(left).equals(Coercion.coerceLong(right));
        } else if (left instanceof Boolean || right instanceof Boolean) {
            return Coercion.coerceBoolean(left).equals(Coercion.coerceBoolean(right));
        } else if (left instanceof java.lang.String || right instanceof String) {
            return left.toString().equals(right.toString());
        }

        return left.equals(right);
    }

    /**
     * Test if left < right.
     * 
     * @param left first value
     * @param right second value
     * @return test result.
     */
    private boolean lessThan(Object left, Object right) {
        if ((left == right) || (left == null) || (right == null)) {
            return false;
        } else if (Coercion.isFloatingPoint(left) || Coercion.isFloatingPoint(right)) {
            double leftDouble = Coercion.coerceDouble(left).doubleValue();
            double rightDouble = Coercion.coerceDouble(right).doubleValue();

            return leftDouble < rightDouble;
        } else if (Coercion.isNumberable(left) || Coercion.isNumberable(right)) {
            long leftLong = Coercion.coerceLong(left).longValue();
            long rightLong = Coercion.coerceLong(right).longValue();

            return leftLong < rightLong;
        } else if (left instanceof String || right instanceof String) {
            String leftString = left.toString();
            String rightString = right.toString();

            return leftString.compareTo(rightString) < 0;
        } else if (left instanceof Comparable) {
            return ((Comparable) left).compareTo(right) < 0;
        } else if (right instanceof Comparable) {
            return ((Comparable) right).compareTo(left) > 0;
        }

        throw new IllegalArgumentException("Invalid comparison : comparing cardinality for left: " + left
            + " and right: " + right);

    }

    /**
     * Test if left > right.
     * 
     * @param left first value
     * @param right second value
     * @return test result.
     */
    private boolean greaterThan(Object left, Object right) {
        if (left == null || right == null) {
            return false;
        }
        return !equals(left, right) && !lessThan(left, right);
    }

    /**
     * Test if left <= right.
     * 
     * @param left first value
     * @param right second value
     * @return test result.
     */
    private boolean lessThanOrEqual(Object left, Object right) {
        return equals(left, right) || lessThan(left, right);
    }

    /**
     * Test if left >= right.
     * 
     * @param left first value
     * @param right second value
     * @return test result.
     */
    private boolean greaterThanOrEqual(Object left, Object right) {
        return equals(left, right) || greaterThan(left, right);
    }

}