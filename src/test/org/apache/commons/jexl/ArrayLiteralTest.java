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

import java.util.Arrays;

import junit.framework.TestCase;

/**
 * Tests Array map literals
 *
 * @author Peter Royal
 * @since 1.2
 */
public class ArrayLiteralTest extends TestCase {

    public void testLiteralWithStrings() throws Exception {
        Expression e = ExpressionFactory.createExpression( "[ 'foo', 'bar' ]" );
        JexlContext jc = JexlHelper.createContext();

        Object o = e.evaluate( jc );
        assertNotNull( o );
        assertTrue( o.getClass().isArray() );
        assertArrayEquals( new Object[]{ "foo", "bar" }, (Object[]) o );
    }

    private void assertArrayEquals( Object[] expected, Object[] actual ) {
        assertTrue( "expected:" + Arrays.asList( expected ) + " but was:" + Arrays.asList( actual ),
                    Arrays.equals( expected, actual ) );
    }

    public void testLiteralWithNumbers() throws Exception {
        Expression e = ExpressionFactory.createExpression( "[ 5, 10 ]" );
        JexlContext jc = JexlHelper.createContext();

        Object o = e.evaluate( jc );
        assertNotNull( o );
        assertTrue( o.getClass().isArray() );
        assertArrayEquals( new Object[]{ new Integer( 5 ), new Integer( 10 ) }, (Object[]) o );
    }

    public void testSizeOfSimpleArrayLiteral() throws Exception {
        Expression e = ExpressionFactory.createExpression( "size([ 'foo' ])" );
        JexlContext jc = JexlHelper.createContext();

        Object o = e.evaluate( jc );
        assertEquals( new Integer( 1 ), o );
    }

    public void testNotEmptySimpleMapLiteral() throws Exception {
        Expression e = ExpressionFactory.createExpression( "empty([ 'foo' ])" );
        JexlContext jc = JexlHelper.createContext();

        Object o = e.evaluate( jc );
        assertFalse( ( (Boolean) o ).booleanValue() );
    }

}
