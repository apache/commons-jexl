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
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.jexl.parser.ASTAddNode;
import org.apache.commons.jexl.parser.ASTArrayAccess;
import org.apache.commons.jexl.parser.ASTAssignment;
import org.apache.commons.jexl.parser.ASTBitwiseAndNode;
import org.apache.commons.jexl.parser.ASTBitwiseComplNode;
import org.apache.commons.jexl.parser.ASTBitwiseOrNode;
import org.apache.commons.jexl.parser.ASTBitwiseXorNode;
import org.apache.commons.jexl.parser.ASTBlock;
import org.apache.commons.jexl.parser.ASTEQNode;
import org.apache.commons.jexl.parser.ASTEmptyFunction;
import org.apache.commons.jexl.parser.ASTExpression;
import org.apache.commons.jexl.parser.ASTExpressionExpression;
import org.apache.commons.jexl.parser.ASTForeachStatement;
import org.apache.commons.jexl.parser.ASTIdentifier;
import org.apache.commons.jexl.parser.ASTIfStatement;
import org.apache.commons.jexl.parser.ASTIntegerLiteral;
import org.apache.commons.jexl.parser.ASTMethod;
import org.apache.commons.jexl.parser.ASTMulNode;
import org.apache.commons.jexl.parser.ASTNullLiteral;
import org.apache.commons.jexl.parser.ASTReference;
import org.apache.commons.jexl.parser.ASTReferenceExpression;
import org.apache.commons.jexl.parser.ASTSizeFunction;
import org.apache.commons.jexl.parser.ASTStatementExpression;
import org.apache.commons.jexl.parser.ASTStringLiteral;
import org.apache.commons.jexl.parser.ASTTrueNode;
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
    /** the stack that holds values during expressions. */
    private Stack valueStack;
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
     * @param node the script or expression to interpret.
     * @param aContext the context to interpret against.
     * @return the result of the interpretation.
     */
    public Object interpret(SimpleNode node, JexlContext aContext) {
        setContext(aContext);
        valueStack = new Stack();
        System.out.println("node is: " + node);
        return node.jjtAccept(this, null);
    }

    /**
     * sets the uberspect to use for divining bean properties etc.
     * @param anUberspect the uberspect.
     */
    public void setUberspect(Uberspect anUberspect) {
        uberspect = anUberspect;
    }

    /** 
     * Gets the uberspect.
     * @return an {@link Uberspect}
     */
    protected Uberspect getUberspect() {
        return uberspect;
    }

    /**
     * Sets the context that contain variables.
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

            // in the event that either is null and not both, then just make the null a 0
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
    public Object visit(ASTArrayAccess node, Object data) {
        node.dump("+");
        return node.childrenAccept(this, data);
    }


    /** {@inheritDoc} */
    public Object visit(ASTAssignment node, Object data) {
        node.dump("+");
        // child 0 should be the variable (reference) to assign to
        Node left = node.jjtGetChild(0);
        Object result = null;
        if (left instanceof ASTReference) {
            ASTReference reference = (ASTReference) left;
            left = reference.jjtGetChild(0);
            // TODO: this only works for a Reference that has a single identifier as it's child
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
        node.dump("+");
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        long l = Coercion.coercelong(left);
        return new Long(~l);
    }
    
    /** {@inheritDoc} */
    public Object visit(ASTBitwiseOrNode node, Object data) { 
        node.dump("+");
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        long l = Coercion.coercelong(left);
        long r = Coercion.coercelong(right);
        return new Long(l | r);
    }

    /** {@inheritDoc} */
    public Object visit(ASTBitwiseXorNode node, Object data) { 
        node.dump("+");
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        long l = Coercion.coercelong(left);
        long r = Coercion.coercelong(right);
        return new Long(l ^ r);
    }

    /** {@inheritDoc} */
    public Object visit(ASTBlock node, Object data) {
        node.dump("+");
        int numChildren = node.jjtGetNumChildren();
        Object result = null;
        for (int i = 0; i < numChildren; i++) {
            result = node.jjtGetChild(i).jjtAccept(this, data);
        }
        return result;
    }

    /** {@inheritDoc} */
    public Object visit(ASTEmptyFunction node, Object data) {
        node.dump("+");
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
        node.dump("+");
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);

        if (left == null && right == null) {
            /*
             * if both are null L == R
             */
            return Boolean.TRUE;
        } else if (left == null || right == null) {
            /*
             * we know both aren't null, therefore L != R
             */
            return Boolean.FALSE;
        } else if (left.getClass().equals(right.getClass())) {
            return left.equals(right) ? Boolean.TRUE : Boolean.FALSE;
        } else if (left instanceof Float || left instanceof Double
                || right instanceof Float || right instanceof Double) {
            Double l = Coercion.coerceDouble(left);
            Double r = Coercion.coerceDouble(right);

            return l.equals(r) ? Boolean.TRUE : Boolean.FALSE;
        } else if (left instanceof Number || right instanceof Number
                || left instanceof Character || right instanceof Character) {
            return Coercion.coerceLong(left).equals(Coercion.coerceLong(right)) ? Boolean.TRUE
                    : Boolean.FALSE;
        } else if (left instanceof Boolean || right instanceof Boolean) {
            return Coercion.coerceBoolean(left).equals(
                    Coercion.coerceBoolean(right)) ? Boolean.TRUE
                    : Boolean.FALSE;
        } else if (left instanceof java.lang.String || right instanceof String) {
            return left.toString().equals(right.toString()) ? Boolean.TRUE
                    : Boolean.FALSE;
        }

        return left.equals(right) ? Boolean.TRUE : Boolean.FALSE;
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
    public Object visit(ASTForeachStatement node, Object data) {
        node.dump("+");

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
            // get an iterator for the collection/array etc via the introspector.
            Iterator itemsIterator = getUberspect().getIterator(
                    iterableValue, DUMMY);
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
    public Object visit(ASTIdentifier node, Object data) { 
        node.dump("+");
        String name = node.image;
        if (data == null) {
            return context.getVars().get(name);
        } else {
            // bean.property, map.key etc

            // TODO: implement map stuff here
            if (data instanceof Map) {
                return ((Map) data).get(name);
            }
            // look up bean property of data and return
            VelPropertyGet vg = getUberspect().getPropertyGet(data, node.image, DUMMY);

            if (vg != null) {
                try {
                    return vg.invoke(data);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                return null;
            }
                
        }
    }
    
    /** {@inheritDoc} */
    public Object visit(ASTIfStatement node, Object data) {
        node.dump("+");
        Object result = null;
        /* first child is the expression */
        Object expression = node.jjtGetChild(0).jjtAccept(this, data);
        if (Coercion.coerceboolean(expression)) {
            // first child is true statement
            result = node.jjtGetChild(1).jjtAccept(this, data);
        } else {
            // if there is a false, execute it. false statement is the second child
            if (node.jjtGetNumChildren() == 3) {
                result = node.jjtGetChild(2).jjtAccept(this, data);
            }
        }

        return result;
    }
    
    /** {@inheritDoc} */
    public Object visit(ASTIntegerLiteral node, Object data) { 
        return Integer.valueOf(node.image); 
    }

    /** {@inheritDoc} */
    public Object visit(ASTMethod node, Object data) {
        node.dump("+"); 
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
            //  DG: If we can't find an exact match, narrow the parameters and try again!
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
    public Object visit(ASTMulNode node, Object data) {
        node.dump("+");
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);

        // the spec says 'and', I think 'or'
        if (left == null && right == null) {
            return new Byte((byte) 0);
        }

        // if anything is float, double or string with ( "." | "E" | "e") coerce
        // all to doubles and do it
        if (left instanceof Float
            || left instanceof Double
            || right instanceof Float
            || right instanceof Double
            || (left instanceof String 
                && (((String) left).indexOf(".") != -1 
                    || ((String) left).indexOf("e") != -1 
                    || ((String) left).indexOf("E") != -1))
            || (right instanceof String 
                && (((String) right).indexOf(".") != -1 
                    || ((String) right).indexOf("e") != -1 
                    || ((String) right).indexOf("E") != -1))) {
            Double l = Coercion.coerceDouble(left);
            Double r = Coercion.coerceDouble(right);

            return new Double(l.doubleValue() * r.doubleValue());
        }

        /*
         * otherwise to longs with thee!
         */

        long l = Coercion.coercelong(left);
        long r = Coercion.coercelong(right);

        return new Long(l * r);
    }


    /** {@inheritDoc} */
    public Object visit(ASTNullLiteral node, Object data) { 
        return null; 
    }

    /** {@inheritDoc} */
    public Object visit(ASTReference node, Object data) {
        // could be array access, identifier or map literal
        // followed by zero or more ("." and array access, method, size, identifier or integer literal)
        node.dump("+");
        int numChildren = node.jjtGetNumChildren();
        Object baseValue = node.jjtGetChild(0).jjtAccept(this, data);
        if (numChildren == 1) {
            return baseValue;
        }
        // pass  first piece of data in and loop through children
        Object result = baseValue;
        for (int i = 1; i < numChildren; i++) {
            result = node.jjtGetChild(i).jjtAccept(this, result);
            // if we get null back a result, stop evaluating the rest of the expr
            if (result == null) {
                break;
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    public Object visit(ASTReferenceExpression node, Object data) {
        node.dump("+");
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTSizeFunction node, Object data) {
        node.dump("+");
        Object val = node.jjtGetChild(0).jjtAccept(this, data);

        if (val == null) {
            throw new IllegalArgumentException("size() : null arg");
        }

        return new Integer(sizeOf(val));
    }

    /** {@inheritDoc} */
    public Object visit(ASTStatementExpression node, Object data) {
        node.dump("+");
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTStringLiteral node, Object data) {
        return node.image;
    }

    /** {@inheritDoc} */
    public Object visit(ASTTrueNode node, Object data) {
        return Boolean.TRUE;
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
     * Calculate the <code>size</code> of various types: Collection, Array, Map, String,
     * and anything that has a int size() method.
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
            //Info velInfo = new Info("", 1, 1);
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
     * Test if the passed value is a floating point number, i.e. a float, double or string with ( "." | "E" | "e").
     * @param val the object to be tested
     * @return true if it is, false otherwise.
     */
    private boolean isFloatingPointNumber(Object val) {
        if (val instanceof Float  || val instanceof Double) {
            return true;
        }
        if (val instanceof String) {
            String string = (String) val;
            return string.indexOf(".") != -1 || string.indexOf("e") != -1 || string.indexOf("E") != -1;
        }
        return false;
    }

}