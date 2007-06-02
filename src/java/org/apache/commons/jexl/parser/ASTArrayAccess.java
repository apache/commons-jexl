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

package org.apache.commons.jexl.parser;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.util.Coercion;
import org.apache.commons.jexl.util.introspection.Info;
import org.apache.commons.jexl.util.introspection.Uberspect;
import org.apache.commons.jexl.util.introspection.VelPropertyGet;

/**
 * Like an ASTIdentifier, but with array access allowed.
 *
 * $foo[2]
 *
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 * @version $Id$
 */
public class ASTArrayAccess extends SimpleNode {
    /** dummy velocity info. */
    private static final Info DUMMY = new Info("", 1, 1);

    /**
     * Create the node given an id.
     *
     * @param id node id.
     */
    public ASTArrayAccess(int id) {
        super(id);
    }

    /**
     * Create a node with the given parser and id.
     *
     * @param p a parser.
     * @param id node id.
     */
    public ASTArrayAccess(Parser p, int id) {
        super(p, id);
    }

    /** {@inheritDoc} */
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    /**
     * evaluate array access upon a base object.
     *
     * foo.bar[2]
     *
     * makes me rethink the array operator :)
     * @param jc the {@link JexlContext} to evaluate against.
     * @param obj not used.
     * @return the value of the array expression.
     * @throws Exception on any error
     */
    public Object execute(Object obj, JexlContext jc) throws Exception {
        ASTIdentifier base = (ASTIdentifier) jjtGetChild(0);

        Object result = base.execute(obj, jc);

        /*
         * ignore the first child - it's our identifier
         */
        for (int i = 1; i < jjtGetNumChildren(); i++) {
            Object loc = ((SimpleNode) jjtGetChild(i)).value(jc);

            if (loc == null) {
                return null;
            }

            result = evaluateExpr(result, loc, getUberspect() );
        }

        return result;
    }

    /** {@inheritDoc} */
    public Object value(JexlContext jc) throws Exception {
        /*
         * get the base ASTIdentifier
         */

        ASTIdentifier base = (ASTIdentifier) jjtGetChild(0);

        Object o = base.value(jc);

        /*
         * ignore the first child - it's our identifier
         */
        for (int i = 1; i < jjtGetNumChildren(); i++) {
            Object loc = ((SimpleNode) jjtGetChild(i)).value(jc);

            if (loc == null) {
                return null;
            }

            o = evaluateExpr(o, loc, getUberspect() );
        }

        return o;
    }

    /**
     * Evaluate the Array expression 'loc' on the given object, o.
     * e.g. in 'a[2]', <code>2</code> is 'loc' and <code>a</code> is 'o'.
     *
     * If o or loc are null, null is returned.
     * If o is a Map, o.get(loc) is returned.
     * If o is a List, o.get(loc) is returned. loc must resolve to an int value.
     * If o is an Array, o[loc] is returned. loc must resolve to an int value.
     * Otherwise loc is treated as a bean property of o.
     *
     * @param o an object to be accessed using the array operator or '.' operator.
     * @param loc the index of the object to be returned.
     * @param uberspect Uberspector to use during evaluation
     * @return the resulting value.
     * @throws Exception on any error.
     */
    public static Object evaluateExpr( Object o, Object loc, Uberspect uberspect ) throws Exception {
        /*
         * following the JSTL EL rules
         */

        if (o == null) {
            return null;
        }

        if (loc == null) {
            return null;
        }

        if (o instanceof Map) {
            if (!((Map) o).containsKey(loc)) {
                return null;
            }

            return ((Map) o).get(loc);
        } else if (o instanceof List) {
            int idx = Coercion.coerceInteger(loc).intValue();

            try {
                return ((List) o).get(idx);
            } catch (IndexOutOfBoundsException iobe) {
                return null;
            }
        } else if (o.getClass().isArray()) {
            int idx = Coercion.coerceInteger(loc).intValue();

            try {
                return Array.get(o, idx);
            } catch (ArrayIndexOutOfBoundsException aiobe) {
                return null;
            }
        } else {
            /*
             * "Otherwise (a JavaBean object)..." huh? :)
             */

            String s = loc.toString();

            VelPropertyGet vg = uberspect.getPropertyGet(o, s, DUMMY);

            if (vg != null) {
                return vg.invoke(o);
            }
        }

        throw new Exception("Unsupported object type for array [] accessor");
    }

    /**
     * Gets the variable name piece of the expression.
     * @return a String of the identifer.
     * @see ASTIdentifier#getIdentifierString().
     */
    public String getIdentifierString() {
        return ((ASTIdentifier) jjtGetChild(0)).getIdentifierString();
    }
}
