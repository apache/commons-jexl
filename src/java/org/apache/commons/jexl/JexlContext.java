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

import java.util.Map;

/**
 * Holds a Map of variables which are referenced in a JEXL expression.
 *
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id: JexlContext.java,v 1.6 2004/08/23 13:53:34 dion Exp $
 */
public interface JexlContext
{
    /**
     * Replaces variables in a JexlContext with the variables contained
     * in the supplied Map.  When setVars() is called on a JexlContext,
     * it clears the current Map and puts each entry of the
     * supplied Map into the current variable Map. 
     * 
     * @param vars Contents of vars will be replaced with the content of this Map
     */
    void setVars(Map vars);
    
    /**
     * Retrives the Map of variables associated with this JexlContext.  The
     * keys of this map correspond to variable names referenced in a
     * JEXL expression.
     * 
     * @return A reference to the variable Map associated with this JexlContext.
     */
    Map getVars();
}
