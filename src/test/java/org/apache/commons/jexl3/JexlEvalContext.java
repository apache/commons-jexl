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

import java.util.Collections;
import java.util.Map;
import org.apache.commons.jexl3.annotations.NoJexl;

/**
 * A JEXL evaluation environment wrapping variables, namespace and options.
 */
public class JexlEvalContext implements
       JexlContext,
       JexlContext.NamespaceResolver,
       JexlContext.OptionsHandle {
    /** The marker for the empty vars. */
    private static final Map<String,Object> EMPTY_MAP = Collections.<String,Object>emptyMap();
    /** The variables.*/
    private final JexlContext vars;
    /** The namespace. */
    private final JexlContext.NamespaceResolver ns; 
    /** The options. */
    private final JexlOptions options = new JexlOptions();

    /**
     * Default constructor.
     */
    @NoJexl
    public JexlEvalContext() {
        this(EMPTY_MAP);
    }
    
    /**
     * Creates an evaluation environment wrapping an existing user provided vars.
     * <p>The supplied vars should be null only in derived classes that override the get/set/has methods.
     * For a default vars context with a code supplied vars, use the default no-parameter contructor.</p>
     * @param map the variables map
     */
    @NoJexl
    public JexlEvalContext(Map<String, Object> map) {
        this.vars = map == EMPTY_MAP ? new MapContext() : new MapContext(map);
        this.ns = null;
    }

    /**
     * Creates an evaluation environment from a context.
     * @param context the context (may be null, implies readonly)
     */
    @NoJexl
    public JexlEvalContext(JexlContext context) {
        this(context, context instanceof JexlContext.NamespaceResolver? (JexlContext.NamespaceResolver) context : null);
    }

    /**
     * Creates an evaluation environment from a context and a namespace.
     * @param context the context (may be null, implies readonly)
     * @param namespace the namespace (may be null, implies empty namespace)
     */
    @NoJexl
    public JexlEvalContext(JexlContext context, JexlContext.NamespaceResolver namespace) {
        this.vars = context != null? context : JexlEngine.EMPTY_CONTEXT;
        this.ns = namespace != null? namespace : JexlEngine.EMPTY_NS;
    }

    @Override
    @NoJexl
    public boolean has(String name) {
        return vars.has(name);
    }

    @Override
    @NoJexl
    public Object get(String name) {
        return vars.get(name);
    }

    @Override
    @NoJexl
    public void set(String name, Object value) {
        vars.set(name, value);
    }

    @Override
    @NoJexl
    public Object resolveNamespace(String name) {
        return ns != null? ns.resolveNamespace(name) : null;
    }

    @Override
    @NoJexl
    public JexlOptions getEngineOptions() {
        return options;
    }

}
