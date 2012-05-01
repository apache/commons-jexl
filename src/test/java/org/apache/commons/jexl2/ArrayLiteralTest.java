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
package org.apache.commons.jexl2;

import java.util.Arrays;

/**
 * Tests for array literals
 * @since 2.0
 */
public class ArrayLiteralTest extends JexlTestCase {

    public void testLiteralWithStrings() throws Exception {
        Expression e = JEXL.createExpression( "[ 'foo' , 'bar' ]" );
        JexlContext jc = new MapContext();

        Object o = e.evaluate( jc );
        Object[] check = { "foo", "bar" };
        assertTrue( Arrays.equals(check, (Object[])o) );
    }

    public void testLiteralWithOneEntry() throws Exception {
        Expression e = JEXL.createExpression( "[ 'foo' ]" );
        JexlContext jc = new MapContext();

        Object o = e.evaluate( jc );
        Object[] check = { "foo" };
        assertTrue( Arrays.equals(check, (Object[])o) );
    }

    public void testLiteralWithNumbers() throws Exception {
        Expression e = JEXL.createExpression( "[ 5.0 , 10 ]" );
        JexlContext jc = new MapContext();

        Object o = e.evaluate( jc );
        Object[] check = { new Double(5), new Integer(10) };
        assertTrue( Arrays.equals(check, (Object[])o) );
        assertTrue (o.getClass().isArray() && o.getClass().getComponentType().equals(Number.class));
    }

    public void testLiteralWithNulls() throws Exception {
        String []exprs = {
            "[ null , 10 ]",
            "[ 10 , null ]",
            "[ 10 , null , 10]",
            "[ '10' , null ]",
            "[ null, '10' , null ]"
        };
        Object [][]checks = {
            {null, new Integer(10)},
            {new Integer(10), null},
            {new Integer(10), null, new Integer(10)},
            { "10", null },
            { null, "10", null }
        };
        JexlContext jc = new MapContext();
        for(int t = 0; t < exprs.length; ++t) {
            Expression e = JEXL.createExpression( exprs[t] );
            Object o = e.evaluate( jc );
            assertTrue(exprs[t], Arrays.equals(checks[t], (Object[])o) );
        }

    }

    public void testLiteralWithIntegers() throws Exception {
        Expression e = JEXL.createExpression( "[ 5 , 10 ]" );
        JexlContext jc = new MapContext();

        Object o = e.evaluate( jc );
        int[] check = { 5, 10 };
        assertTrue( Arrays.equals(check, (int[])o) );
    }

    public void testSizeOfSimpleArrayLiteral() throws Exception {
        Expression e = JEXL.createExpression( "size([ 'foo' , 'bar' ])" );
        JexlContext jc = new MapContext();

        Object o = e.evaluate( jc );
        assertEquals( new Integer( 2 ), o );
    }

    public void notestCallingMethodsOnNewMapLiteral() throws Exception {
        Expression e = JEXL.createExpression( "size({ 'foo' : 'bar' }.values())" );
        JexlContext jc = new MapContext();

        Object o = e.evaluate( jc );
        assertEquals( new Integer( 1 ), o );
    }

    public void testNotEmptySimpleArrayLiteral() throws Exception {
        Expression e = JEXL.createExpression( "empty([ 'foo' , 'bar' ])" );
        JexlContext jc = new MapContext();

        Object o = e.evaluate( jc );
        assertFalse( ( (Boolean) o ).booleanValue() );
    }

}
