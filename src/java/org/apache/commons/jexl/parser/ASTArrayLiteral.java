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

import org.apache.commons.jexl.JexlContext;

public class ASTArrayLiteral extends SimpleNode {
    public ASTArrayLiteral( int id ) {
        super( id );
    }

    public ASTArrayLiteral( Parser p, int id ) {
        super( p, id );
    }

    /** Accept the visitor. * */
    public Object jjtAccept( ParserVisitor visitor, Object data ) {
        return visitor.visit( this, data );
    }

    public Object value( JexlContext context ) throws Exception {
        int childCount = jjtGetNumChildren();
        Object[] array = new Object[childCount];

        for ( int i = 0; i < childCount; i++ ) {
            array[i] = ( (SimpleNode) jjtGetChild( i ) ).value( context );
        }

        return array;
    }
}
