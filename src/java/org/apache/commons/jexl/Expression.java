/*
 * Copyright 2002,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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


/**
 *  Inferface for expression object.
 *
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id: Expression.java,v 1.4 2004/02/28 13:45:20 yoavs Exp $
 */
public interface Expression
{
    /**
     *  Evaluates the expression, returning the return value;
     */
    public Object evaluate(JexlContext context) throws Exception;

    /**
     *  returns the expression used
     */
    public String getExpression();

    /**
     *  allows addition of a resolver to allow custom interdiction of
     *  expression evaluation
     *
     *  @param resolver resolver to be called before Jexl expression evaluated
     */
    public void addPreResolver(JexlExprResolver resolver);

    /**
     *  allows addition of a resolver to allow custom interdiction of
     *  expression evaluation
     *
     *  @param resolver resolver to be called if Jexl expression evaluated to null
     */
    public void addPostResolver(JexlExprResolver resolver);
}
