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

import org.apache.commons.jexl.context.HashMapContext;

/**
 *  Helper to create contexts.  Really no reason right now why you just can't
 *  instantiate the HashMapContext on your own, but maybe we make this return
 *  a context factory to let apps override....
 *
 *  Then you can do all sorts of goofy contexts (backed by databases, LDAP, etc)
 *
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id: JexlHelper.java,v 1.3 2004/02/28 13:45:20 yoavs Exp $
 */
public class JexlHelper
{
    protected static JexlHelper helper = new JexlHelper();

    public static JexlContext createContext()
    {
        return getInstance().newContext();
    }

    protected JexlContext newContext()
    {
        return new HashMapContext();
    }

    protected static JexlHelper getInstance()
    {
        return helper;
    }

}
